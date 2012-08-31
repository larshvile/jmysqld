package net.hulte.jmysqld;

/**
 * A running instance of the MySQL server.
 */
public interface MySqlServerInstance {

    // TODO getPort()? -- kind of hard if a config file is used??

    /**
     * Returns {@code true} while the instance is running.
     */
    boolean isRunning();

    /**
     * Shuts down the instance.
     *
     * @throws MySqlProcessException if unable to shut down
     */
    void shutdown();

}

