package net.hulte.jmysqld;

import static net.hulte.jmysqld.Utilities.*;

import java.nio.file.Path;
import java.util.Set;

/**
 * Specifications used to start a new instance of the MySQL server.
 */
public final class MySqlServerInstanceSpecs {

    public enum Option {

        /** TODO docme */
        SHUTDOWN_EXISTING,

        /** TODO me too */
        AUTO_SHUTDOWN
    }

    private final Set<Option> options = set();
    private Integer port;
    private Path defaultsFile;


    // TODO factory method for creating an instance in a 'test profile?' .. SHUTDOWN_EXISTING + AUTO_SHUTDOWN

    public static MySqlServerInstanceSpecs newInstanceSpecs(Option... options) {
        final MySqlServerInstanceSpecs result = new MySqlServerInstanceSpecs();
        for (Option o : options) {
            result.option(o);
        }
        return result;
    }


    public MySqlServerInstanceSpecs() {
    }


    MySqlServerInstanceSpecs option(Option o) {
        options.add(o);
        return this;
    }

    boolean isSet(Option o) {
        return options.contains(o);
    }

    /**
     * The port number that the server should use when listening for TCP/IP connections. If neither the port
     * nor a {@code defaultsFile} is provided, the instance will be started with {@code --skip-networking}.
     */
    MySqlServerInstanceSpecs port(Integer port) {
        this.port = port;
        return this;
    }

    Integer getPort() {
        return port;
    }

    /**
     * An option file to be read instead of the usual option files. Equivalent of the mysqld_safe
     * {@code --defaults-file} option. If this file is not specified the server will be started without
     * reading any option files, i.e. {@code --no-defaults}.
     */
    MySqlServerInstanceSpecs defaultsFile(Path defaultsFile) {
        this.defaultsFile = defaultsFile;
        return this;
    }

    Path getDefaultsFile() {
        return defaultsFile;
    }
}

