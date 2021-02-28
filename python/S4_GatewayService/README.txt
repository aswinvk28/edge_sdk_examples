Instructions on how to adjust the terminal's height for displaying all the samples of S4_GatewayService example:

/**********************************************************************************************************************/
LINUX INSTRUCTION:

From the terminal window, run the following command to adjust the terminal size:

    resize -s <ROWS> <COLUMNS>

For example, the command:

    resize -s 45 $COLUMNS
    
will set the terminal height to 45 and use the current "COLUMNS" size as terminal width.

The required terminal height depends on the running time of the example application.
The longer you run the example for, the more terminal height you would require.


/**********************************************************************************************************************/
WINDOWS INSTRUCTION:

From the terminal window, run the following command to adjust the terminal height:

    mode con lines=<LINES>

For example, the command:

    mode con lines=45
    
will set the terminal height to 45.

The required terminal height depends on the running time of the example application.
The longer you run the example for, the more terminal height you would require.
