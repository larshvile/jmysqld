package net.hulte.jmysqld;

import java.nio.file.Path;

/**
 * Provides factory methods for obtaining {@link MySqlServer} instances.
 */
public final class MySql {

    /**
     * Creates a {@link MySqlServer} based on a path containing a binary distribution of MySQL.
     *
     * @throws IllegalArgumentException if {@code distPath} does not contain the MySQL binaries
     */
    public static MySqlServer mySqlServerFromBinaryDistribution(Path distPath) {
        return new BinaryDistributionMySqlServer(distPath);
    }

    private MySql() {}
}

