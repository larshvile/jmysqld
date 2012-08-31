package net.hulte.jmysqld;

import java.nio.file.Path;

/**
 * The MySQL server application.
 */
public interface MySqlServer {

    /**
     * Returns the server version.
     *
     * @throws MySqlProcessException
     * @throws IllegalStateException if the version string can't be parsed
     */
    String getVersion();

    /**
     * Initializes a MySQL data directory, the equivalent of running the
     * {@code mysql_install_db} script.
     *
     * @param dataDir the data directory
     * @throws MySqlProcessException
     */
    void initializeDataDirectory(Path dataDir);

    /**
     * Starts an instance of the MySQL server in a provided data directory. This
     * method blocks until the server is fully operational.
     *
     * @throws MySqlProcessException if unable to start
     */
    MySqlServerInstance start(Path dataDir); // TODO options .. and probably some more.. specified port?

    /**
     * Returns {@code true} if an instance of the MySQL server is  running
     * in a provided data directory.
     *
     * @throws MySqlProcessException
     */
    boolean isInstanceRunningIn(Path dataDir);

    /**
     * Attempts to shut down an instance of the MySQL server running in a provided
     * data directory.
     *
     * @throws MySqlProcessException
     */
    void shutdownInstanceIn(Path dataDir);

}

