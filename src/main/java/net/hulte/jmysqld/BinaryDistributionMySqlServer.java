package net.hulte.jmysqld;

import static java.nio.file.Files.exists;
import static net.hulte.jmysqld.Utilities.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Pattern;
import java.util.*;

final class BinaryDistributionMySqlServer implements MySqlServer {

    private static final Pattern versionPattern = Pattern.compile("^.*Ver\\s(.*)\\sfor.*");

    private final Path distPath;

    BinaryDistributionMySqlServer(Path distPath) {
        this.distPath = distPath.toAbsolutePath();
        if (!exists(mysqldPath())) {
            throw new IllegalArgumentException("mysqld binary not found at at "
                + mysqldPath() + ".");
        }
    }

    @Override
    public String getVersion() {
        final String output = collectOutput("--version");
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

    private String collectOutput(String... args) { // TODO el fixo
        final ProcessBuilder pb = mysqldProcessBuilder(args);

        try {
            final Process p = pb.start();
            p.waitFor();

            if (p.exitValue() != 0) { // TODO custom exception please..
                throw new RuntimeException("mysqld exited with code "
                    + p.exitValue() + ", error: "
                    + readText(p.getErrorStream()));
            }

            return readText(p.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException(e); // TODO get some control on this
        }
    }

    private ProcessBuilder mysqldProcessBuilder(String... args) {
        final List<String> processAndArgs = list(mysqldPath().toString());
        processAndArgs.addAll(list(args));
        return new ProcessBuilder(processAndArgs);
    }
}

