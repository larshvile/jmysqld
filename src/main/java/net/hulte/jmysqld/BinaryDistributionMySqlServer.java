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

        if (isInstanceRunningIn(dataDir)) {
            throw new MySqlProcessException("Another instance is already running in "
                + dataDir + ".");
            /*
            logger.warn("Another instance is already running in " + dataDir
                + ", attempting to shut it down.");
            shutdownInstanceIn(dataDir); // TODO this should probably not be the default behaviour... option?
            */
        }

        logger.debug("Starting MySQL in " + dataDir + ".");

        final Path errorLog = dataDir.resolve("error.log");

        final ProcessBuilder pb = newProcessBuilder(mysqldSafe(),
            "--no-defaults", // TODO or specific file if provided as option??
            "--skip-networking", // TODO unless a port is specified
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
        while (!isInstanceRunningIn(dataDir)) { // TODO, actually this isn't good enough.. another instance could already be running there .. good enough if we checked earlier that it wasn't running though..
            if (!instance.isRunning()) {
                throw new MySqlProcessException("Failed to start instance, see "
                    + errorLog + " for details.");
            }
            try {
                Thread.sleep(500); // TODO tune me...
            }  catch (InterruptedException e) { // TODO yet another one of these.. fix it?
                throw new RuntimeException(e);
            }
        }

        return instance;
    }

    @Override
    public boolean isInstanceRunningIn(Path dataDir) {
        return startMySqlProcess(newProcessBuilder(mysqladmin(),
                "--socket=" + socket(dataDir),
                "ping"))
            .waitForCompletion()
            .exitCode() == 0;
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
            shutdownInstanceIn(dataDir); // TODO not really bulletproof.. especially if !isRunning()
            try {
                running.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MySqlProcessException("Interrupted while waiting for shutdown.", e);
            }
        }
    }
}

