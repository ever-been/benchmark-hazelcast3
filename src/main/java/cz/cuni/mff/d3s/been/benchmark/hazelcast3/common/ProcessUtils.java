package cz.cuni.mff.d3s.been.benchmark.hazelcast3.common;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Martin Sixta
 */
public final class ProcessUtils {

	private ProcessUtils() {

	}

	public static int run(String line) {
		return run(line, ".");
	}

	public static int run(String line, String relativeWorkingDirectory) {
		return runCommand(line, relativeWorkingDirectory, true, true);
	}

	public static int run(String line, String relativeWorkingDirectory, boolean stdout) {
		return runCommand(line, relativeWorkingDirectory, stdout, false);
	}

	public static int run(String line, String relativeWorkingDirectory, boolean stdout, boolean stderr) {
		return runCommand(line, relativeWorkingDirectory, stdout, stderr);
	}

	private static int runCommand(String line, String relativeWorkingDirectory, boolean stdout, boolean stderr) {
		CommandLine cmdLine = CommandLine.parse(line);
		DefaultExecutor executor = new DefaultExecutor();

		OutputStream stdOutStream = System.out;
		OutputStream stdErrStream = System.err;

		if (!stdout) {
			stdOutStream = null;
		}

		if (!stderr) {
			stdErrStream = null;
		}

		executor.setStreamHandler(new PumpStreamHandler(stdOutStream, stdErrStream));
		executor.setWorkingDirectory(new File(relativeWorkingDirectory));

		try {
			return executor.execute(cmdLine);
		} catch (IOException e) {
			e.printStackTrace();
			return -1;

		}

	}
}
