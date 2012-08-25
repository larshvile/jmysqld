package net.hulte.jmysqld;

import static net.hulte.jmysqld.MySql.*;
import static org.junit.Assert.*;

import java.nio.file.*;

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(JUnit4.class)
public class MySqlTest {

    @Test(expected=IllegalArgumentException.class)
    public void server_cannot_be_created_from_a_folder_that_does_not_contain_the_binary_distribution() {
        mySqlServerFromBinaryDistribution(distPath().getParent());
    }


    @Test
    public void server_is_created_from_binary_distribution() {
        final MySqlServer server = mySqlServerFromBinaryDistribution(distPath());
        assertNotNull(server);
    }


    static Path distPath() {
        final String version = System.getProperty("mysqlVersion");
        if (version == null) {
            throw new IllegalStateException("MySQL version (alias) must be provided as a system property, "
                + "-DmysqlVersion=<alias>");
        }
        return FileSystems.getDefault().getPath("mysql-bin").resolve(version);
    }
}

