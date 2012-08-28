package net.hulte.jmysqld;

import static java.nio.file.Files.exists;
import static net.hulte.jmysqld.MySqlProcess.startMySqlProcess;
import static net.hulte.jmysqld.Utilities.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.file.*;
import java.util.regex.Pattern;
import java.util.List;
import org.slf4j.Logger;

final class BinaryDistributionMySqlServer implements MySqlServer {

    private static final Pattern versionPattern = Pattern.compile("^.*Ver\\s(.*)\\sfor.*");

    private final Logger logger = getLogger(getClass());
    private final Path distPath;

    BinaryDistributionMySqlServer(Path distPath) {
        this.distPath = distPath.toAbsolutePath();
        if (!exists(mysqldPath())) {
            throw new IllegalArgumentException("mysqld binary not found at "
                + mysqldPath() + ".");
        }
    }

    @Override
    public String getVersion() {
        final String output = startMySqlProcess(mysqld("--version"))
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

        startMySqlProcess(mysqlInstallDb("--basedir=" + distPath, "--datadir=" + dataDir,
                "--user=" + userName()))
            .logStdOutWith(logger)
            .waitForSuccessfulCompletion();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + distPath;
    }

    private ProcessBuilder mysqld(String... args) {
        return newProcessBuilder(mysqldPath(), args);
    }

    private ProcessBuilder mysqlInstallDb(String... args) {
        return newProcessBuilder(distPath.resolve("scripts").resolve("mysql_install_db"),
            args);
    }

    private Path mysqldPath() {
        return distPath.resolve("bin").resolve("mysqld");
    }
}

