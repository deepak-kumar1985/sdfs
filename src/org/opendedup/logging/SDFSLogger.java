package org.opendedup.logging;

import java.io.IOException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.opendedup.sdfs.Main;

public class SDFSLogger {

	private static Logger log = Logger.getLogger("sdfs");
	private static Logger awslog = Logger.getLogger("com.amazonaws");
	private static Logger fslog = Logger.getLogger("fs");
	private static Logger basicLog = Logger.getLogger("bsdfs");
	private static boolean debug = false;
	private static boolean fsdebug = false;
	static {

		ConsoleAppender bapp = new ConsoleAppender(new PatternLayout("%m%n"));
		basicLog.addAppender(bapp);

		basicLog.setLevel(Level.WARN);
		RollingFileAppender app = null;
		try {

			app = new RollingFileAppender(new PatternLayout(
					"%d [%c] [%C] [%L] [%t] %x - %m%n"), Main.logPath, true);
			app.setMaxBackupIndex(2);
			app.setMaxFileSize("10MB");
		} catch (IOException e) {
			log.debug("unable to change appender", e);
		}
		awslog.setLevel(Level.WARN);
		awslog.removeAllAppenders();
		log.setLevel(Level.INFO);
		fsdebug = true;
		fslog.setLevel(Level.INFO);
		Logger rootLogger = Logger.getRootLogger();
		rootLogger.setLevel(Level.INFO);
		rootLogger.addAppender(app);
	}

	public static Logger getLog() {
		return log;

	}

	public static Logger getFSLog() {
		return fslog;
	}

	public static void infoConsoleMsg(String msg) {
		System.out.println(msg);
		log.info(msg);
	}

	public static Logger getBasicLog() {
		return basicLog;
	}

	public static boolean isDebug() {
		return debug;
	}

	public static boolean isFSDebug() {
		return fsdebug;
	}

	public static void setLevel(int level) {
		if (level == 0) {
			log.setLevel(Level.DEBUG);
			debug = true;
		} else {
			log.setLevel(Level.INFO);
			debug = false;
		}
	}

	public static void setFSLevel(int level) {
		if (level == 0) {
			fslog.setLevel(Level.DEBUG);
			fsdebug = true;
		} else {
			fslog.setLevel(Level.INFO);
			fsdebug = false;
		}
	}

	public static void setToFileAppender(String file) {
		try {
			log.removeAllAppenders();
		} catch (Exception e) {

		}
		RollingFileAppender app = null;
		try {
			app = new RollingFileAppender(new PatternLayout(
					"%d [%c] [%t] %x - %m%n"), file, true);
			app.setMaxBackupIndex(2);
			app.setMaxFileSize("10MB");
		} catch (IOException e) {
			System.out.println("Unable to initialize logger");
			e.printStackTrace();
		}
		log.addAppender(app);
		fslog.addAppender(app);
		log.setLevel(Level.INFO);
		fslog.setLevel(Level.DEBUG);
	}
}
