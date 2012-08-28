package net.hulte.jmysqld;

import static net.hulte.jmysqld.Utilities.*;

import java.io.*;
import java.util.List;
import org.slf4j.Logger;

/**
 * Provides a simple framework for working with the MySQL binaries. Wraps a regular {@link Process} by
 * exposing a more high-level API.
 */
final class MySqlProcess {

    private final Process p;
    private final List<String> command;


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
            if (p.exitValue() != 0) {
                throw new MySqlProcessException("Failed to run '"
                    + command + "', exit-code: " + p.exitValue()
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
        try {
            return readText(p.getInputStream());
        } catch (IOException e) {
            throw new MySqlProcessException("Unable to read stdout.", e);
        }
    }

    /**
     * Starts a thread that continously logs the text written to stdout by the process using a
     * provided logger.
     */
    MySqlProcess logStdOutWith(final Logger logger) {
        final Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final BufferedReader r = asReader(p.getInputStream());
                    String line = null;
                    while (true) {
                        line = r.readLine();
                        if (line == null) {
                            break;
                        }
                        logger.trace(line);
                    }
                } catch (IOException e) {
                    logger.warn("Unable to read stdout.", e);
                }
            }
        });

        t.setDaemon(true);
        t.start();

        return this;
    }
}

