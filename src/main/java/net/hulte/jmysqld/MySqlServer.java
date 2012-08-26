package net.hulte.jmysqld;

/**
 * The MySQL server application.
 */
public interface MySqlServer {

    /**
     * Returns the server version.
     *
     * @throws MySqlProcessException if the MySQL process can't be invoked
     * @throws IllegalStateException if the version string can't be parsed
     */
    String getVersion();

    /**
     * TODO starts an instance & returns when startup has completed??
     */
    // MySqlServerProcess start(); // TODO dataDir & settings .. a builder perhaps??

}

