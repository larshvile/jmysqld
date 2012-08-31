package net.hulte.jmysqld;

import static net.hulte.jmysqld.Utilities.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;

/**
 * Provides a simple framework for working with the MySQL binaries. Wraps a regular {@link Process} by
 * exposing a more high-level API.
 */
final class MySqlProcess {

    private final Process p;
    private final List<String> command;
    private CountDownLatch logging;


    static MySqlProcess startMySqlProcess(ProcessBuilder pb) {
        try {
            return new MySqlProcess(pb.start(), pb.command());
        } catch (IOException e) {
            throw new MySqlProcessException("Unable to start " + pb.command() + ".", e);
        }
    }

    private MySqlProcess(Process p, List<String> command) {
        this.p = p;
        this.command = command;
    }


    /**
     * Waits for the process to successfully complete.
     *
     * @throws MySqlProcessException if interrupted while waiting, or if the process exits
     *      with an error code.
     */
    MySqlProcess waitForSuccessfulCompletion() {
        waitForCompletion();
        try {
            if (exitCode() != 0) {
                throw new MySqlProcessException("Failed to run '"
                    + command + "', exit-code: " + exitCode()
                    + ", error: " + readText(p.getErrorStream()));
            }

            return this;
        } catch (IOException e) {
            throw new MySqlProcessException("Failed to run '" + command + "'.", e);
        }
    }

    /**
     * Waits for the process to complete.
     *
     * @throws MySqlProcessException if interrupted while waiting
     */
    MySqlProcess waitForCompletion() {
        try {
            p.waitFor();
            flushLogs();
            return this;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MySqlProcessException("Interrupted while waiting for " + p, e);
        }
    }

    /**
     * Returns the text written to stdout by the process.
     */
    String readStdOut() {
        return readProcessStream(p.getInputStream());
    }

    /**
     * Returns the text written to stderr by the process.
     */
    String readStdErr() {
        return readProcessStream(p.getErrorStream());
    }

    /**
     * Returns the exit-code of the process after it has terminated.
     */
    int exitCode() {
        return p.exitValue();
    }

    /**
     * Starts a thread that continously logs the text written to stdout by the process using a
     * provided logger.
     */
    MySqlProcess logStdOut() {
        final Logger logger = processLogger();

        startNamedDaemon("stdout-logger-for-" + processName(), new Runnable() {
            @Override public void run() {
                logging = new CountDownLatch(1);
                try {
                    final BufferedReader r = asReader(p.getInputStream());
                    String line = null;
                    while (true) {
                        line = r.readLine();
                        if (line == null) {
                            break;
                        }
                        logger.debug(line);
                    }
                } catch (IOException e) {
                    logger.warn("Unable to read stdout.", e);
                } finally {
                    logging.countDown();
                }
            }
        });

        return this;
    }

    private String readProcessStream(InputStream in) {
        try {
            return readText(in);
        } catch (IOException e) {
            throw new MySqlProcessException("Unable to output from process.", e);
        }
    }

    private Logger processLogger() {
        return getLogger(this.getClass().getPackage().getName()
            + ".#" + processName());
    }

    private String processName() {
        return new File(command.get(0)).toPath().getFileName().toString();
    }

    private void flushLogs() throws InterruptedException {
        if (logging != null) {
            logging.await();
        }
    }
}

