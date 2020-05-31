/***
    Copyright 2020 by Sean Luke
    Licensed under the Apache License version 2.0
*/

package flow.utilities;
import java.io.*;
import java.util.*;

public class StringUtilities
    {
    public static String read(File file)
        {
        try
            {
            return read(new FileInputStream(file));
            }
        catch (Exception ex)
            {
            ex.printStackTrace();
            return null;
            }
        }
                
    public static String read(InputStream stream)
        {
        try
            {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder build = new StringBuilder();
            char[] buf = new char[1024];
                
            while(true)
                {
                int val = reader.read(buf, 0, buf.length);
                if (val < 0) break;
                build.append(buf, 0, val);
                }
            return build.toString();
            }
        catch (Exception ex)
            {
            ex.printStackTrace();
            return null;
            }
        }
    }
