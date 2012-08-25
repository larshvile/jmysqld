package net.hulte.jmysqld;

import java.nio.file.*;

final class BinaryDistributionMySqlServer implements MySqlServer {

    final Path distPath;

    BinaryDistributionMySqlServer(Path distPath) {
        this.distPath = distPath.toAbsolutePath();
        if (!mysqldPath().toFile().exists()) {
            throw new IllegalArgumentException("mysqld binary not found at at " + mysqldPath() + ".");
        }
    }

    @Override
    public String getVersion() {
        return null; // TODO
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + distPath;
    }

    private Path mysqldPath() {
        return distPath.resolve("bin").resolve("mysqld");
    }
}

