package net.hulte.jmysqld;

import java.nio.file.Path;

/**
 * Provides factory methods for obtaining {@link MySqlServer} instances.
 */
public final class MySql {

    /**
     * Creates a {@link MySqlServer} based on a path containing a binary distribution of MySQL.
     */
    public static MySqlServer mySqlServerFromBinaryDistribution(Path distributionPath) {
        return new BinaryDistributionMySqlServer(distributionPath);
    }

    private MySql() {}
}

