package net.hulte.jmysqld;

import static net.hulte.jmysqld.MySql.*;
import static net.hulte.jmysqld.Utilities.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.junit.*;
import org.junit.rules.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(JUnit4.class)
public class MySqlTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();


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
    public void the_server_version_is_obtained() {
        assertThat(theServer().getVersion(), equalTo(mysqlVersion()));
    }

    @Test
    public void empty_folder_is_initialized_with_mysql_data_files() throws Exception {
        final File dataDir = tmp.getRoot();

        assertThat(contents(dataDir), not(hasItem("mysql")));

        theServer().initializeDataDirectory(dataDir.toPath());

        assertThat(contents(dataDir), hasItem("mysql"));
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

    static List<String> contents(File f) {
        return list(f.list());
    }
}

