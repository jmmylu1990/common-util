package org.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;

public class CMDUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(CMDUtils.class);
	private static final String OS_NAME = "os.name";
	private static final String OS_CHARSET = isWindowsOS() ? "Big5" : StandardCharsets.UTF_8.name();
	
	private CMDUtils() {
	}

	/**
	 * 執行CMD指令
	 *
	 * @param cmd
	 *            CMD指令, String型別
	 * @return CMD執行結果, String型別
	 */
	public static String exec(String cmd) {
		StringBuilder sb = new StringBuilder();
		sb.append("<cmd>\r\n").append(cmd).append("\r\n</cmd>\r\n");
		try {
			String[] cmdArguments = getCmdArguments(cmd);
			Process process = new ProcessBuilder(cmdArguments).start();
			try (BufferedReader errorIn = new BufferedReader(new InputStreamReader(process.getErrorStream(), OS_CHARSET));
					BufferedReader outputIn = new BufferedReader(new InputStreamReader(process.getInputStream(), OS_CHARSET))) {
				int errIndex = 0;
				String line = null;
				while ((line = errorIn.readLine()) != null) {
					if (++errIndex > 0) {
						sb.append("<error>\r\n");
					}
					sb.append(line).append("\r\n");
				}
				if (errIndex > 0) {
					sb.append("</error>\r\n");
				}

				sb.append("<output>\r\n");
				while ((line = outputIn.readLine()) != null) {
					sb.append(line).append("\r\n");
				}
				sb.append("</output>\r\n\r\n");
			}
			sb.append("Process exitValue: ").append(process.waitFor());
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return sb.toString();
	}
	
	public static boolean isWindowsOS() {
		return System.getProperty(OS_NAME).toLowerCase().contains("win");
	}

	public static boolean isUnixOS() {
		return System.getProperty(OS_NAME).toLowerCase().contains("nix");
	}

	public static boolean isMacOS() {
		return System.getProperty(OS_NAME).toLowerCase().contains("mac");
	}

	public static String[] getCmdArguments(String... cmds) {
		String[] baseArgs = isWindowsOS() ? 
					new String[] { "cmd", "/c" } :
					new String[] { "/bin/sh", "-c" };
		return Stream.concat(Arrays.stream(baseArgs), Arrays.stream(cmds)).toArray(String[]::new);
	}
}
