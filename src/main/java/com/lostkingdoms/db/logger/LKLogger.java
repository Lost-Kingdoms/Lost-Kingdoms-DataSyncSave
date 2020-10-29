package com.lostkingdoms.db.logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.common.io.Files;

/**
 * Name: LKLogger Description: Logger class to handle outputs
 *
 * @author FPetersen
 */
public class LKLogger {

	private static LKLogger instance;

	public static LKLogger getInstance() {
		if (instance == null)
			instance = new LKLogger();
		return instance;
	}

	/**
	 * Constructor for the Logger (creates the file if needed)
	 */
	public LKLogger() {
		File mkdir = new File(getConfigPath());
		File tmpFile = new File(getConfigPath() + File.separator + "logs.txt");

		try {
			if (!mkdir.exists())
				mkdir.mkdirs();
			if (!tmpFile.exists()) {

				tmpFile.createNewFile();
			} else {
				File copyFile = new File(getConfigPath() + File.separator + "logs-"
						+ new Date().toInstant().getNano() + ".txt");
				Files.copy(tmpFile, copyFile);
				tmpFile.delete();
				tmpFile.createNewFile();

			}
			setPrintWriter(new PrintWriter(tmpFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private LogLevel level = LogLevel.INFO;
	private PrintWriter stream = null;
	private boolean active = true;
	private boolean consoleOutput = true;
	private LogType logType = LogType.ALL;

	public void setLevel(LogLevel l) {
		level = l;
	}

	public String getConfigPath() {
		String currentPath = new File(".").getAbsolutePath().substring(0, new File(".").getAbsolutePath().length() - 1);
        return currentPath + "config" + File.separator + "LKDBTest";
	}
	
	/**
	 * Set the type of the {@link LKLogger}
	 *
	 * @param logType
	 */
	public void setLogType(LogType logType) {
		this.logType = logType;
	}

	/**
	 * Set the output writer for a file
	 *
	 * @param f
	 */
	public void setPrintWriter(PrintWriter f) {
		stream = f;
	}

	/**
	 * Enable or disable the logging system
	 *
	 * @param active
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	public void error(Object m, LogType lt) {
		if (LogLevel.ERROR.compareTo(level) <= 0) {
			format("ERROR", m, lt);
		}
	}

	public void error(Object m, Throwable e, LogType lt) {
		if (LogLevel.ERROR.compareTo(level) <= 0) {
			StringBuilder sb = new StringBuilder(1024);
			sb.append(m);
			sb.append(ExceptionUtils.getStackTrace(e));

			format("ERROR", sb.toString(), lt);
		}
	}

	public void warn(Object m, LogType lt) {
		if (LogLevel.WARNING.compareTo(level) <= 0) {
			format("WARNING", m, lt);
		}
	}

	public void warn(Object m, Throwable e, LogType lt) {
		if (LogLevel.WARNING.compareTo(level) <= 0) {
			StringBuilder sb = new StringBuilder(1024);
			sb.append(m);
			sb.append(ExceptionUtils.getStackTrace(e));

			format("WARNING", sb.toString(), lt);
		}
	}

	public void debug(Object m, LogType lt) {
		if (LogLevel.DEBUG.compareTo(level) <= 0) {
			format("DEBUG", m, lt);
		}
	}

	public void info(Object m, LogType lt) {
		if (LogLevel.INFO.compareTo(level) <= 0) {
			format("INFO", m, lt);
		}
	}

	public void trace(Object m, LogType lt) {
		if (LogLevel.TRACE.compareTo(level) <= 0) {
			format("TRACE", m, lt);
		}
	}

	public void format(String type, Object m, LogType lt) {
		if (!active)
			return;
		if (!lt.equals(LogType.ALL) && lt.equals(logType))
			return;

		StringBuilder sb = new StringBuilder(1024);
		sb.append("[");
		sb.append(type);
		sb.append("] ");
		sb.append(" - ");
		sb.append("[");
		sb.append(lt.name());
		sb.append("] ");
		sb.append(new Date().toString());
		sb.append(" - ");
		sb.append(m);

		String line = sb.toString();
		if (consoleOutput) {
			System.out.println(line);
		}

		if (stream != null) {
			stream.println(line);
			stream.flush();
		}

	}
}
