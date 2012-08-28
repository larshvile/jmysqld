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
     * TODO starts an instance & returns when startup has completed??
     */
    // MySqlServerInstance start(); // TODO dataDir & settings .. a builder perhaps??

}

