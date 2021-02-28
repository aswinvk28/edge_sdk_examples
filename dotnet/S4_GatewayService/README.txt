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

Run the start_gatewayservice.bat file to adjust the terminal height:

    start_gatewayservice.bat <RUNNING_TIME> <TERMINAL_HEIGHT>

Here, passing <TERMINAL_HEIGHT> will set the terminal's height to the given number.

For example, the command:

    start_gatewayservice.bat 60 50

will run the application for 60 seconds and set the terminal height to 50.

By default this example application run for 60 seconds and terminal height is 45 when started via the batch file.

The required terminal height depends on the running time of the example application.
The longer you run the example for, the more terminal height you would require.
