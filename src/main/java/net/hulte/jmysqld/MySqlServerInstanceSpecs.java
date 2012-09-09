package net.hulte.jmysqld;

import static net.hulte.jmysqld.Utilities.*;

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
    // TODO port
    // TODO defaultsFile


    // TODO factory method for creating an instance in a 'test profile?' .. SHUTDOWN_EXISTING + AUTO_SHUTDOWN


    public MySqlServerInstanceSpecs() {
    }


    MySqlServerInstanceSpecs option(Option o) {
        options.add(o);
        return this;
    }

    boolean isSet(Option o) {
        return options.contains(o);
    }
}

