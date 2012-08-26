package net.hulte.jmysqld;

import static java.nio.file.Files.exists;
import static net.hulte.jmysqld.Utilities.*;

import java.nio.file.*;
import java.util.regex.Pattern;
import java.util.List;

final class BinaryDistributionMySqlServer implements MySqlServer {

    private static final Pattern versionPattern = Pattern.compile("^.*Ver\\s(.*)\\sfor.*");

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
        final String output = collectOutput(mysqldProcess("--version"));
        final String version = firstMatchOrNull(versionPattern, output);
        if (version == null) {
            throw new IllegalStateException("Unable to parse version-string, "
                + output);
        }
        return version;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + distPath;
    }

    private Path mysqldPath() {
        return distPath.resolve("bin").resolve("mysqld");
    }

    private ProcessBuilder mysqldProcess(String... args) {
        final List<String> processAndArgs = list(mysqldPath().toString());
        processAndArgs.addAll(list(args));
        return new ProcessBuilder(processAndArgs);
    }
}

