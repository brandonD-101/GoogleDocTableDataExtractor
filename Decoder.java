import java.util.HashMap;

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
    /**
     * takes a URL which has a table of data in the format x, charecter, y, and prints it to the terminal, 
     * 0,0 is bottom left of the image.
     * runs in O(max(x) + max(y)).
     */
    public static void printData(String url)
    {
        HashMap<Position,String> dataMap = new HashMap<Position,String>();
        
        int maxWidth = 0;
        int maxHeight = 0;
        
        Document doc = null;
        
        try
        {
            doc = Jsoup.connect(url).get();
        }
        catch (java.io.IOException ioe)
        {
            ioe.printStackTrace();
            System.out.println("IOException occured, double check the url");
        }
        
        boolean isFirstRow = true;
        for(Element row : doc.select("tr"))
        {
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