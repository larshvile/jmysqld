package net.hulte.jmysqld;

/**
 * The MySQL server application.
 */
public interface MySqlServer {

    /**
     * Returns the server version.
     */
    String getVersion();

    /**
     * TODO starts an instance & returns when startup has completed??
     */
    // MySqlServerProcess start(); // TODO dataDir & settings .. a builder perhaps??

}

