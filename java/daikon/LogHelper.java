package daikon;

import java.util.logging.*;

/**
 * Standard methods for setting up logging.
 * Allows creation of Console writers using one method.
 * Logger methods should only be called in a shell class
 * at setup, after which Logger calls should be used
 * for logging.
 **/
public final class LogHelper {
  private LogHelper() { throw new Error("do not instantiate"); }

  // Class variables so user doesn't have to use "Level." prefix.
  public static final Level FINE = Level.FINE;
  public static final Level INFO = Level.INFO;
  public static final Level WARNING = Level.WARNING;
  public static final Level SEVERE = Level.SEVERE;

  /**
   * Sets up global logs with a given priority and logging output
   * pattern.  Creates one ConsoleHandler at root to receive default
   * messages, setting priority to INFO.  Removes previous appenders
   * at root.
   **/
  public static void setupLogs(Level l, Formatter formatter) {
    // Send debug and other info messages to System.err
    Handler app = new ConsoleHandler();
    app.setLevel (Level.ALL);
    app.setFormatter(formatter);

    // Logger.global.removeAllAppenders();
    {
      Handler[] handlers = Logger.global.getHandlers();
      for (int i=0; i<handlers.length; i++) {
        Logger.global.removeHandler(handlers[i]);
      }
    }


    Logger root = Logger.getLogger ("");
    Handler[] handlers = root.getHandlers();
    for (int i=0; i < handlers.length; i++)
      root.removeHandler(handlers[i]);
    root.addHandler (app);
    root.setLevel(l);

    // Logger.global.addHandler(app);
    // Logger.global.setLevel(l);
    // Logger.global.fine ("Installed logger at level " + l);
  }

  // Statically initialized to save runtime
  private static String[] padding_arrays = new String[] {
    "",
    " ",
    "  ",
    "   ",
    "    ",
    "     ",
    "      ",
    "       ",
    "        ",
    "         ",
    "          ",
    "           ",
    "            ",
    "             ",
    "              ",
    "               ",
    "                ",
    "                 ",
    "                  ",
    "                   ",
  };

  public static class DaikonLogFormatter extends SimpleFormatter {
    public String format(LogRecord record) {
      // // By default, take up 20 spaces min, and 20 spaces max for logger.
      // // %c = Logger. %m = message, %n = newline
      // // Example output: "@ daikon.Daikon: This is a message"
      // setupLogs (l, "@ %20.20c: %m%n");

      String loggerName = record.getLoggerName() + ":";
      int len = loggerName.length();
      if (len > 20) {
        loggerName = loggerName.substring(len - 20, len);
      } else if (len < 20) {
        loggerName = loggerName + padding_arrays[20 - len];
      }

      // If we aren't generating tracebacks, include the src class/method
      String src = "";
      if (!Debug.dkconfig_showTraceback)
        src = record.getSourceClassName().replaceAll ("\\w*\\.", "")
                + "." + record.getSourceMethodName() + ": ";

      return "@ " + loggerName + " " + src + record.getMessage()
             + Global.lineSep;
    }
  }


  /**
   * Default method for setting up global logs.
   **/
  public static void setupLogs() {
    setupLogs (INFO);
  }

  /**
   * Sets up global logs with a given priority.
   * Creates one ConsoleHandler.  Removes previous appenders at root.
   **/
  public static void setupLogs(Level l) {
    setupLogs (l, new DaikonLogFormatter());
  }

  /**
   * Changes the logging priority of a sub category.
   **/
  public static void setLevel(String s, Level l) {
    Logger.getLogger(s).setLevel(l);
  }

}
