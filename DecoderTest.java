import java.util.HashMap;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DecoderTest
{
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        runAll();
    }

    public static void runAll() {
        System.out.println("=== DecoderTest Suite ===\n");

        testConvertToJsoupUrl();
        testConvertToJsoupUrlWithTab();
        testConvertToJsoupUrlNoChange();

        testArrow();
        testHeart();
        testCat();
        testHouse();

        testGoogleDocUrl();

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.out.println("SOME TESTS FAILED");
        } else {
            System.out.println("ALL TESTS PASSED");
        }
    }

    // --- URL conversion tests ---

    public static void testConvertToJsoupUrl() {
        String input = "https://docs.google.com/document/d/abc123/edit?tab=t.0";
        String expected = "https://docs.google.com/document/d/abc123/export?format=html";
        String result = Decoder.convertToJsoupUrl(input);
        assertEq("convertToJsoupUrl basic", expected, result);
    }

    public static void testConvertToJsoupUrlWithTab() {
        String input = "https://docs.google.com/document/d/1fJpT8z19OIlF6N-f86yBFVZ-vehcWxpHr79_qhe6lTc/edit?tab=t.0";
        String expected = "https://docs.google.com/document/d/1fJpT8z19OIlF6N-f86yBFVZ-vehcWxpHr79_qhe6lTc/export?format=html";
        String result = Decoder.convertToJsoupUrl(input);
        assertEq("convertToJsoupUrl with real doc ID", expected, result);
    }

    public static void testConvertToJsoupUrlNoChange() {
        String input = "https://docs.google.com/document/d/abc123/export?format=html";
        String result = Decoder.convertToJsoupUrl(input);
        assertEq("convertToJsoupUrl already converted", input, result);
    }

    // --- Local file rendering tests ---

    public static void testArrow() {
        runFileTest("Arrow", "test_data/test_arrow_x_char_y.html", "test_data/expected_arrow.txt");
    }

    public static void testHeart() {
        runFileTest("Heart", "test_data/test_heart_x_char_y.html", "test_data/expected_heart.txt");
    }

    public static void testCat() {
        runFileTest("Cat", "test_data/test_cat_x_char_y.html", "test_data/expected_cat.txt");
    }

    public static void testHouse() {
        runFileTest("House", "test_data/test_house_x_char_y.html", "test_data/expected_house.txt");
    }

    public static void testGoogleDocUrl() {
        String url = "https://docs.google.com/document/d/1fJpT8z19OIlF6N-f86yBFVZ-vehcWxpHr79_qhe6lTc/edit?tab=t.0";
        String testName = "Google Doc (House - X,Y,Character column order)";

        System.out.println("  [INFO] " + testName);
        System.out.println("  [INFO] This doc uses X, Y, Character column order.");
        System.out.println("  [INFO] The current Decoder expects x, character, y order.");
        System.out.println("  [INFO] This test documents the incompatibility.");

        String converted = Decoder.convertToJsoupUrl(url);
        String expectedConverted = "https://docs.google.com/document/d/1fJpT8z19OIlF6N-f86yBFVZ-vehcWxpHr79_qhe6lTc/export?format=html";
        assertEq(testName + " URL conversion", expectedConverted, converted);

        try {
            Document doc = Jsoup.connect(converted).get();
            Elements rows = doc.select("tr");
            boolean hasRows = rows.size() > 1;
            assertCondition(testName + " has table data", hasRows);

            Element headerRow = rows.first();
            Elements headerCells = headerRow.select("td");
            if (headerCells.size() >= 3) {
                String col0 = headerCells.get(0).text().trim();
                String col1 = headerCells.get(1).text().trim();
                String col2 = headerCells.get(2).text().trim();
                System.out.println("  [INFO] Detected columns: [" + col0 + ", " + col1 + ", " + col2 + "]");
                assertCondition(testName + " col order differs from x,char,y",
                    col1.equalsIgnoreCase("Y") || col1.equalsIgnoreCase("y"));
            }
        } catch (Exception e) {
            System.out.println("  [SKIP] " + testName + " network fetch: " + e.getMessage());
        }
    }

    // --- Helpers ---

    private static void runFileTest(String name, String htmlPath, String expectedPath) {
        String testName = "Render " + name;
        try {
            String expectedOutput = Files.readString(Path.of(expectedPath)).stripTrailing();

            String captured = captureOutput(htmlPath);
            String[] capturedLines = captured.split("\n");

            StringBuilder artOnly = new StringBuilder();
            boolean foundStart = false;
            for (String line : capturedLines) {
                if (line.startsWith("starting parsing")) {
                    foundStart = true;
                    continue;
                }
                if (line.startsWith("doc loaded status")) {
                    continue;
                }
                if (foundStart) {
                    if (artOnly.length() > 0) {
                        artOnly.append("\n");
                    }
                    artOnly.append(line);
                }
            }

            String actual = artOnly.toString().stripTrailing();

            String[] expectedLines = expectedOutput.split("\n");
            String[] actualLines = actual.split("\n");

            assertEq(testName + " line count", String.valueOf(expectedLines.length), String.valueOf(actualLines.length));

            boolean allMatch = true;
            for (int i = 0; i < Math.min(expectedLines.length, actualLines.length); i++) {
                if (!expectedLines[i].stripTrailing().equals(actualLines[i].stripTrailing())) {
                    System.out.println("  [DETAIL] Line " + i + " mismatch:");
                    System.out.println("    expected: \"" + expectedLines[i].stripTrailing() + "\"");
                    System.out.println("    actual:   \"" + actualLines[i].stripTrailing() + "\"");
                    allMatch = false;
                }
            }
            assertCondition(testName + " content matches", allMatch);

        } catch (Exception e) {
            System.out.println("  [FAIL] " + testName + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
    }

    private static String captureOutput(String filePath) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(baos);
        System.setOut(capture);
        try {
            Decoder.printData(filePath);
        } finally {
            System.setOut(originalOut);
        }
        return baos.toString();
    }

    private static void assertEq(String testName, String expected, String actual) {
        if (expected.equals(actual)) {
            System.out.println("  [PASS] " + testName);
            passed++;
        } else {
            System.out.println("  [FAIL] " + testName);
            System.out.println("    expected: \"" + expected + "\"");
            System.out.println("    actual:   \"" + actual + "\"");
            failed++;
        }
    }

    private static void assertCondition(String testName, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            passed++;
        } else {
            System.out.println("  [FAIL] " + testName);
            failed++;
        }
    }
}
