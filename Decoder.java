import java.util.HashMap;
import java.io.File;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
/**
 *  takes a URL which is a google docs that has a table of data in the format x, charecter, y, and prints it to the terminal, 
 *  0,0 is bottom left of the image.
 *  
 *  parse each row into a Data object, updating width and heigt when bounds are exceeded
 *  table row format:  x, charecter, y
 *  
 *  by Brandon Dickson 29/06/2026
 */
public class Decoder
{
    public static void main(String[] args){
        for(String str : args){
            printData(str);
        }
    }
    public static void test(){
        printData("https://docs.google.com/document/d/1m5zw3lyK_ZyaRxLBdB9mlCr-KpT_foiuXH-8m-fKhnw/edit?tab=t.0");
    }
    public static String convertToJsoupUrl(String originalUrl) {
        // Finds the document ID and replaces everything after it with the HTML export command
        return originalUrl.replaceAll("/edit.*$", "/export?format=html");
    }
    /**
     * takes a URL which has a table of data in the format x, charecter, y, and prints it to the terminal, 
     * 0,0 is bottom left of the image.
     * runs in O(max(x) + max(y)).
     */
    public static void printData(String url)
    {
        boolean ishttp = url.contains("http");
        if(ishttp){
            url = convertToJsoupUrl(url);
        }
        HashMap<Position,String> dataMap = new HashMap<Position,String>();
        
        int maxWidth = 0;
        int maxHeight = 0;
        
        Document doc = null;
        System.out.println("starting parsing...");
        
        try
        {
            if(!ishttp){
                doc = Jsoup.parse(new File(url));
                //System.out.println("is file: "+doc.text());
            }
            else
            {
                doc = Jsoup.connect(url).get();
            }
            System.out.println("doc loaded status: " + (doc==null?"null":"good"));
        }
        catch (java.io.IOException ioe)
        {
            ioe.printStackTrace();
            System.out.println("IOException occured, double check the url");
        }
        //System.out.println("doc:"+doc.text());
        
        boolean isFirstRow = true;
        for(Element row : doc.select("tr"))
        {
            //System.out.println("printing row :" + row);
            //skip header row, first row is always first
            if(isFirstRow)
            {
                isFirstRow = false;
                continue;
            }
            
            //parse each row into a Data object, updating width and heigt when bounds are exceeded
            //table row format:  x, charecter, y
            
            int index = 0; //keeps track of what part of the data we are currently updating
            String data = null;
            int x=0;
            int y=0;
            for(Element rawData : row.select("td"))
            {
                if(index == 0){
                    x =  Integer.parseInt(rawData.text());
                    if(x > maxWidth){
                        maxWidth = x;
                    }
                }
                else if(index == 1){
                    data = rawData.text();
                }
                else {
                    y =  Integer.parseInt(rawData.text());
                    if(y > maxHeight){
                        maxHeight = y;
                    }
                    
                }
                index++;
            }
            
            if(data!=null){
                dataMap.put(new Position(x,y),data);
            }
        }
        
        //print the data to the teminal line by line
        for(int y = maxHeight;y>=0;y--){
            String rowToPrint = "";
            for(int x = 0;x<=maxWidth;x++)
            {
                String data = dataMap.get(new Position(x,y));
                if( data != null){
                    rowToPrint+=data;
                }
                else{
                    rowToPrint+=" ";  
                }
            }
            System.out.println(rowToPrint);
        }
    }
}