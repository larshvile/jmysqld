package net.hulte.jmysqld;

import static java.nio.file.Files.exists;
import static net.hulte.jmysqld.MySqlServerInstanceSpecs.Option.*;
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

    @Override
    public MySqlServerInstance start(Path dataDir, MySqlServerInstanceSpecs specs) {

        if (isInstanceRunningIn(dataDir)) {
            if (!specs.isSet(SHUTDOWN_EXISTING)) {
                throw new MySqlProcessException("Another instance is already running in "
                    + dataDir + ".");
            }

            logger.debug("Another instance is already running in " + dataDir
                + ", attempting to shut it down.");
            shutdownInstanceIn(dataDir);
        }

        logger.debug("Starting MySQL in " + dataDir + ".");

        final Path errorLog = dataDir.resolve("error.log");
        final List<String> args = list("--no-defaults", // TODO or specific file if provided as option??
                "--basedir=" + distPath,
                "--datadir=" + dataDir,
                "--socket=" + socket(dataDir),
                "--pid-file=mysql.pid",
                "--log-error=" + errorLog);

        if (specs.getPort() != null) {
            args.add("--port=" + specs.getPort());
        } else { // TODO if (defaultsFile == null)
            args.add("--skip-networking");
        }

        final ProcessBuilder pb = newProcessBuilder(mysqldSafe(), args);
        pb.directory(distPath.toFile());
        pb.redirectErrorStream(true);

        final MySqlProcess p = startMySqlProcess(pb).logStdOut();
        final MySqlServerInstance instance = new BinaryDistributionMySqlServerInstance(p, dataDir,
                specs.isSet(AUTO_SHUTDOWN));

        // TODO eh, let's improve this =)
        while (!isInstanceRunningIn(dataDir)) {
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
        // TODO log it?
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

        BinaryDistributionMySqlServerInstance(final MySqlProcess p, Path dataDir, boolean autoShutdown) {
            this.dataDir = dataDir;

            startNamedDaemon("mysqld-monitor-" + dataDir, new Runnable() {
                @Override public void run() {
                    p.waitForCompletion();
                    // TODO log it
                    running.countDown();
                }
            });

            if (autoShutdown) {
                addShutdownHook(new Runnable() {
                    @Override public void run() {
                        System.out.println("AUTO-SHUTDOWN"); // TODO log it instead?
                        shutdown();
                    }
                });
            }
        }

        @Override
        public boolean isRunning() {
            return running.getCount() == 1;
        }

        @Override
        public void shutdown() {
            if (!isRunning()) { // TODO not really bulletproof.. especially if !isRunning()
                return;
            }

            shutdownInstanceIn(dataDir);
            try {
                running.await();    // TODO fixme
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MySqlProcessException("Interrupted while waiting for shutdown.", e);
            }
        }
    }
}

