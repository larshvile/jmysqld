package net.hulte.jmysqld;

import java.nio.file.Path;

/**
 * The MySQL server application.
 *
 * <p>Note that any method requiring a data directory will <b>not</b> work reliably
 * unless the client can guarantee that no other thread, jvm or other process
 * (e.g. a native mysqld) is also working with that directory.</p>
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
     * @param dataDir the data directory
     * @param specs specifications of how to launch the new instance
     * @throws MySqlProcessException if unable to start
     */
    MySqlServerInstance start(Path dataDir, MySqlServerInstanceSpecs specs);

    /**
     * Returns {@code true} if an instance of the MySQL server is running
     * in the provided data directory. This only works reliably if the instance
     * running in the provided directory was started using {@code jmysqld}.
     *
     * @param dataDir the data directory
     * @throws MySqlProcessException
     */
    boolean isInstanceRunningIn(Path dataDir);

    /**
     * Attempts to shut down an instance of the MySQL server running in the provided
     * data directory. This only works reliably if the instance running in the provided
     * directory was started using {@code jmysqld}.
     *
     * @param dataDir the data directory
     * @throws MySqlProcessException
     */
    void shutdownInstanceIn(Path dataDir);

}

