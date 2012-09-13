package net.hulte.jmysqld;

import static java.nio.file.Files.exists;
import static net.hulte.jmysqld.InstanceSpec.Option.*;
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
    public MySqlServerInstance start(Path dataDir, InstanceSpec spec) {
        logger.debug("Starting MySQL in " + dataDir + ".");

        final Path errorLog = dataDir.resolve("error.log");
        final List<String> args = startArguments(dataDir, errorLog, spec);
        final ProcessBuilder pb = newProcessBuilder(mysqldSafe(), args);

        pb.directory(distPath.toFile());
        pb.redirectErrorStream(true);

        final MySqlProcess p = startMySqlProcess(pb)
            .logStdOut();

        final BinaryDistributionMySqlServerInstance instance = new BinaryDistributionMySqlServerInstance(p,
                dataDir, spec.isSet(AUTO_SHUTDOWN));
        instance.awaitStartup();
        return instance;
    }

    private List<String> startArguments(Path dataDir, Path errorLog, InstanceSpec spec) {
        final String defaultsOption = spec.getDefaultsFile() == null
                ? "--no-defaults"
                : "--defaults-file=" + spec.getDefaultsFile();

        final List<String> result = list(
                defaultsOption,
                "--basedir=" + distPath,
                "--datadir=" + dataDir,
                "--socket=" + socket(dataDir),
                "--pid-file=mysql.pid",
                "--log-error=" + errorLog);

        if (spec.getPort() != null) {
            result.add("--port=" + spec.getPort());
        } else if (spec.getDefaultsFile() == null) {
            result.add("--skip-networking");
        }

        return result;
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
        final CountDownLatch running = new CountDownLatch(1);

        BinaryDistributionMySqlServerInstance(final MySqlProcess p, final Path dataDir,
                boolean autoShutdown) {

            this.dataDir = dataDir;

            startNamedDaemon("mysqld-monitor-" + dataDir, new Runnable() {
                @Override public void run() {
                    p.waitForCompletion();
                    logger.trace("Instance in " + dataDir + " exited.");
                    running.countDown();
                }
            });

            if (autoShutdown) {
                addShutdownHook(new Runnable() {
                    @Override public void run() {
                        if (isRunning()) { // TODO if the sockets were unique there would be no need for the monitor-thread & the current impl of isRunning() .. doing the regular ping would be good enough
                            logger.debug("Automatically shutting down instance in " + dataDir + ".");
                            shutdown();
                        }
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
            if (!isRunning()) {
                return;
            }

            shutdownInstanceIn(dataDir);

            execute(new Interruptible() {
                @Override public void run() throws InterruptedException {
                    running.await();
                }
            });
        }

        void awaitStartup() {
            execute(new Interruptible() {
                @Override public void run() throws InterruptedException {
                    while (!isInstanceRunningIn(dataDir)) { // TODO kind of confusing to read this stuf..
                        if (!isRunning()) {
                            throw new MySqlProcessException(
                                "Failed to start instance, see the error-log for details.");
                        }
                        Thread.sleep(100);
                    }
                }
            });
        }
    }
}

