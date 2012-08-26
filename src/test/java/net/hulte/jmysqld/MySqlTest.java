package net.hulte.jmysqld;

import static net.hulte.jmysqld.MySql.*;
import static net.hulte.jmysqld.Utilities.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

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

    @Test
    public void server_version_is_obtained() {
        assertThat(theServer().getVersion(), equalTo(mysqlVersion()));
    }

    static MySqlServer theServer() {
        return mySqlServerFromBinaryDistribution(distPath());
    }

    static Path distPath() {
        return path("mysql-bin").resolve(mysqlVersion());
    }

    static String mysqlVersion() {
        final String version = System.getProperty("mysqlVersion");
        if (version == null) {
            throw new IllegalStateException("MySQL version must be provided as a system property, "
                + "-DmysqlVersion=<version>");
        }
        return version;
    }
}

