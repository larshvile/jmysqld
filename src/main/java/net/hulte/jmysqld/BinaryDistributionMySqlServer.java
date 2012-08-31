package net.hulte.jmysqld;

import static java.nio.file.Files.exists;
import static net.hulte.jmysqld.MySqlProcess.startMySqlProcess;
import static net.hulte.jmysqld.Utilities.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import org.slf4j.Logger;

final class BinaryDistributionMySqlServer implements MySqlServer {

    private static final Pattern versionPattern = Pattern.compile("^.*Ver\\s(.*)\\sfor.*");

    private final Logger logger = getLogger(getClass());
    private final Path distPath;

    BinaryDistributionMySqlServer(Path distPath) {
        this.distPath = distPath.toAbsolutePath();
        if (!exists(mysqld())) {
            throw new IllegalArgumentException("mysqld binary not found at " + mysqld() + ".");
        }
    }

    @Override
    public String getVersion() {
        final String output = startMySqlProcess(newProcessBuilder(mysqld(), "--version"))
            .waitForSuccessfulCompletion()
            .readStdOut();

        final String version = firstMatchOrNull(versionPattern, output);

        if (version == null) {
            throw new IllegalStateException("Unable to parse version-string, "
                + output);
        }

        return version;
    }

    @Override
    public void initializeDataDirectory(Path dataDir) {
        logger.debug("Initializing data-directory " + dataDir + ".");

        startMySqlProcess(newProcessBuilder(mysqlInstallDb(),
                "--basedir=" + distPath,
                "--datadir=" + dataDir,
                "--user=" + userName()))
            .logStdOut()
            .waitForSuccessfulCompletion();
    }

    /* TODO some startup-options are required..
     * SHUTDOWN_EXISTING, AUTO_SHUTDOWN, SETTINGS_IN_DATADIR (otherwise it's no-settings & no-port!)
     */
    @Override
    public MySqlServerInstance start(Path dataDir) {  // TODO dataDir/settings.. builder??

        // TODO wipe the log if it exists...? failures to start could then list stderr as well as error.log

        // TODO should this really be the default behaviour??
            // TODO .. nope .. options options options
        if (isInstanceRunningIn(dataDir)) {
            logger.warn("Another instance is already running in " + dataDir
                + ", attempting to shut it down.");
            shutdownInstanceIn(dataDir);
        }

        logger.debug("Starting MySQL in " + dataDir + ".");

        // TODO check out the no-defaults & defaults-file stuff...

        final Path errorLog = dataDir.resolve("error.log");

        final ProcessBuilder pb = newProcessBuilder(mysqldSafe(),
            "--basedir=" + distPath,
            "--datadir=" + dataDir,
            "--socket=" + socket(dataDir),
            "--pid-file=mysql.pid",
            "--log-error=" + errorLog
            );

        pb.directory(distPath.toFile());
        pb.redirectErrorStream(true);

        final MySqlProcess p = startMySqlProcess(pb).logStdOut();
        final MySqlServerInstance instance = new BinaryDistributionMySqlServerInstance(p, dataDir);

        // TODO eh, let's improve this =)
        while (!isInstanceRunningIn(dataDir)) {
            if (!instance.isRunning()) {
                throw new MySqlProcessException("Failed to start instance, see "
                    + errorLog + " for details.");
            }
            try {
                Thread.sleep(500); // TODO tune me...
            }  catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return instance;
    }

    @Override
    public boolean isInstanceRunningIn(Path dataDir) {
        final MySqlProcess p = startMySqlProcess(newProcessBuilder(mysqladmin(),
                "--socket=" + socket(dataDir),
                "ping"))
            .waitForCompletion();

        if (p.exitCode() == 0) {
            logger.trace("Instance is running in " + dataDir + ", " + p.readStdOut());
            return true;
        } else {
            logger.trace("Instance is not running in " + dataDir + ", " + p.readStdErr());
            return false;
        }
    }

    @Override
    public void shutdownInstanceIn(Path dataDir) {
        startMySqlProcess(newProcessBuilder(mysqladmin(),
                "--socket=" + socket(dataDir),
                "--user=root",
                "shutdown"))
            .waitForSuccessfulCompletion();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + distPath;
    }

    private Path mysqld() {
        return distPath.resolve("bin").resolve("mysqld");
    }

    private Path mysqldSafe() {
        return distPath.resolve("bin").resolve("mysqld_safe");
    }

    private Path mysqlInstallDb() {
        return distPath.resolve("scripts").resolve("mysql_install_db");
    }

    private Path mysqladmin() {
        return distPath.resolve("bin").resolve("mysqladmin");
    }

    private static Path socket(Path dataDir) {
        return dataDir.resolve("mysql.sock");
    }


    private class BinaryDistributionMySqlServerInstance implements MySqlServerInstance {

        final Path dataDir;
        final CountDownLatch running = new CountDownLatch(1); // TODO should be initialized within the monitor instead?

        BinaryDistributionMySqlServerInstance(final MySqlProcess p, Path dataDir) {
            this.dataDir = dataDir;

            startNamedDaemon("mysqld-monitor-" + dataDir, new Runnable() {
                @Override public void run() {
                    p.waitForCompletion();
                    running.countDown();
                }
            });
        }

        @Override
        public boolean isRunning() {
            return running.getCount() == 1;
        }

        @Override
        public void shutdown() {
            shutdownInstanceIn(dataDir);
            try {
                running.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MySqlProcessException("Interrupted while waiting for shutdown.", e);
            }
        }
    }
}

