package net.hulte.jmysqld;

import static java.nio.file.Files.exists;
import static java.util.UUID.randomUUID;
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
        final Path socket = newSocket();

    // TODO extract method?
        final List<String> args = startArguments(dataDir, socket, errorLog, spec);
        final ProcessBuilder pb = newProcessBuilder(mysqldSafe(), args);

        pb.directory(distPath.toFile());
        pb.redirectErrorStream(true);

        final MySqlProcess p = startMySqlProcess(pb)
            .logStdOut();

        final BinaryDistributionMySqlServerInstance instance = new BinaryDistributionMySqlServerInstance(p,
                dataDir, socket, spec.isSet(AUTO_SHUTDOWN));
    // TODO extract?
        instance.awaitStartup();
        return instance;
    }

    private List<String> startArguments(Path dataDir, Path socket, Path errorLog, InstanceSpec spec) {
        final String defaultsOption = spec.getDefaultsFile() == null
                ? "--no-defaults"
                : "--defaults-file=" + spec.getDefaultsFile();

        final List<String> result = list(
                defaultsOption,
                "--basedir=" + distPath,
                "--datadir=" + dataDir,
                "--socket=" + socket,
                "--pid-file=mysql.pid",
                "--log-error=" + errorLog);

        if (spec.getPort() != null) {
            result.add("--port=" + spec.getPort());
        } else if (spec.getDefaultsFile() == null) {
            result.add("--skip-networking");
        }

        return result;
    }

    private class BinaryDistributionMySqlServerInstance implements MySqlServerInstance {

        final Path socket;
        final CountDownLatch processExited = new CountDownLatch(1);

        BinaryDistributionMySqlServerInstance(final MySqlProcess p, final Path dataDir, Path socket,
                boolean autoShutdown) {

            this.socket = socket;

            startNamedDaemon("mysqld-monitor-" + dataDir, new Runnable() {
                @Override public void run() {
                    try {
                        p.waitForCompletion();
                    } finally {
                        logger.trace("Instance in " + dataDir + " shut down.");
                        processExited.countDown();
                    }
                }
            });

            if (autoShutdown) {
                addShutdownHook(new Runnable() {
                    @Override public void run() {
                        if (isRunning()) {
                            logger.trace("Automatically shutting down instance in " + dataDir + ".");
                            shutdown();
                        }
                    }
                });
            }
        }

        @Override
        public boolean isRunning() {
            return startMySqlProcess(newProcessBuilder(mysqladmin(),
                    "--socket=" + socket,
                    "ping"))
                .waitForCompletion()
                .exitCode() == 0;
        }

        @Override
        public void shutdown() {
            if (!isRunning()) {
                return;
            }

            startMySqlProcess(newProcessBuilder(mysqladmin(),
                    "--socket=" + socket,
                    "--user=root",
                    "shutdown"))
                .waitForSuccessfulCompletion();

            await(processExited);
        }

        void awaitStartup() {
            while (!isRunning()) {
                if (processExited.getCount() == 0) {
                    throw new MySqlProcessException(
                        "Failed to start instance, see the error-log for details.");
                }
                sleep(100);
            }
        }
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

    private static Path newSocket() {
        return tmpDir().resolve(randomUUID().toString());
    }
}

