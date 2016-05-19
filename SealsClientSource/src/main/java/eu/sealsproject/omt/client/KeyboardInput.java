package eu.sealsproject.omt.client;

import java.io.* ;
/**
 *  A simple input class to read values typed at the command line.  If
 *  an error occurs during input, any exceptions thrown are caught and
 *  a default value returned.
 *  
 *  It has been modified (maybe) for its use in the client.
 *
 *  @version 1.0 1.7.97
 *  @author Graham Roberts
 */
public class KeyboardInput
{
    /**
     *  Read an int value from keyboard input.
     */
    public final synchronized int readInteger()
    {
        //
        //  The place to hold the data received from the keyboard so
        //  that we can parse it to find the int value.
        //
        String input = "" ;
        //
        //  Read a String of characters from the keyboard.
        //
        try
        {
            input = in.readLine() ;
        }
        catch (IOException e) {}
        //
        //  Parse the String to construct an int value.
        //
        int val = 0 ;
        try
        {
            val = Integer.parseInt(input) ;
        }
        catch (NumberFormatException e) {}
        return val ;
    }
     /**
     *  Read a long value from keyboard input.
     */
    public final synchronized long readLong()
    {
        //
        //  The place to hold the data received from the keyboard so
        //  that we can parse it to find the long value.
        //
        String input = "" ;
        //
        //  Read a String of characters from the keyboard.
        //
        try
        {
            input = in.readLine() ;
        }
        catch (IOException e) {}
        //
        //  Parse the String to construct a long value.
        //
        long val = 0L ;
        try
        {
            val = Long.parseLong(input) ;
        }
        catch (NumberFormatException e) {}
        return val ;
    }
     /**
     *  Read a double value from keyboard input.
     */
    public final synchronized double readDouble()
    {
        //
        //  The place to hold the data received from the keyboard so
        //  that we can parse it to find the double value.
        //
        String input = "" ;
        //
        //  Read a String of characters from the keyboard.
        //
        try
        {
            input = in.readLine() ;
        }
        catch (IOException e) {}
        //
        //  Parse the String to construct a double value.
        //
        double val = 0.0D ;
        try
        {
            val = (Double.valueOf(input)).doubleValue() ;
        }
        catch (NumberFormatException e) {}
        return val ;
    }
    /**
     *  Read a float value from keyboard input.
     */
    public final synchronized float readFloat()
    {
        //
        //  The place to hold the data received from the keyboard so
        //  that we can parse it to find the long value.
        //
        String input = "" ;
        //
        //  Read a String of characters from the keyboard.
        //
        try
        {
            input = in.readLine() ;
        }
        catch (IOException e) {}
        //
        //  Parse the String to construct a float value.
        //
        float val = 0.0F ;
        try
        {
            val = (Float.valueOf(input)).floatValue() ;
        }
        catch (NumberFormatException e) {}
        return val ;
    }
    /**
     *  Read a char value from keyboard input.
     */
    public final synchronized char readCharacter()
    {
        //
        //  No need to parse anything, just get a character and return
        //  it..
        //
        char c = ' ' ;
        try
        {
            c = (char)in.read() ;
        }
        catch (IOException e) {}
        return c ;
    }
    /**
     *  Read an String value from keyboard input.
     */
    public final synchronized String readString()
    {
        //
        //  No need to parse anything, just get a string and return
        //  it..
        //
        String s = "";
        try
        {
            s = in.readLine() ;
        }
        catch (IOException e) {}
        return s ;
    }
    /**
     *  The stream that is the keyboard wrapped so that we can read
     *  from it sensibly.
     */
    private final BufferedReader in =
        new BufferedReader(new InputStreamReader(System.in)) ;
}

 

