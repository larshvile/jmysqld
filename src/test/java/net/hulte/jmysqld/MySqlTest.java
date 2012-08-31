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
public class MySqlTest { // TODO really just a test for the binary-dist version... rename?

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder(); // TODO make that a 'named' folder instead, tmp/$className/$version


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
    @Ignore // TODO fixo
    public void empty_folder_is_initialized_with_mysql_data_files() throws Exception {
        assertThat(contents(dataDir()), not(hasItem("mysql")));

        theServer().initializeDataDirectory(dataDir());

        assertThat(contents(dataDir()), hasItem("mysql"));
    }

    @Test
    @Ignore
    public void server_starts_up() {    // TODO what about some jdbc-testing?
        final MySqlServer server = theServer();
        // TODO final Path dataDir = tmp.getRoot().toPath();
        final Path dataDir = new File("/home/lars/Desktop/mysql-datadir").toPath();

        // TODO theServer().initializeDataDirectory(dataDir);
        theServer().start(dataDir); // TODO AUTO_SHUTDOWN
        // TODO what now?

        try {
            Thread.sleep(2000);
        } catch (Exception e) { throw new RuntimeException(e); }

        // TODO connect to it & have some fun
    }

    @Test
    public void server_can_be_started_and_stopped_via_datadir() {
        final MySqlServer server = theServer();
        final Path dataDir = dataDir(); // new File("/home/lars/Desktop/mysql-datadir-test").toPath();

        assertFalse(server.isInstanceRunningIn(dataDir));

        server.initializeDataDirectory(dataDir);
        server.start(dataDir);

        assertTrue(server.isInstanceRunningIn(dataDir));

        server.shutdownInstanceIn(dataDir);

        assertFalse(server.isInstanceRunningIn(dataDir));
    }

    @Test
    public void server_can_be_started_and_stopped_via_instance() {
        final MySqlServer server = theServer();

        assertFalse(server.isInstanceRunningIn(dataDir()));

        server.initializeDataDirectory(dataDir());
        final MySqlServerInstance instance = server.start(dataDir()); // TODO AUTO_SHUTDOWN

        assertTrue(instance.isRunning());
        assertTrue(server.isInstanceRunningIn(dataDir()));

        instance.shutdown();

        assertFalse(instance.isRunning());
        assertFalse(server.isInstanceRunningIn(dataDir()));
    }


    Path dataDir() {
        return tmp.getRoot().toPath();
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

    static List<String> contents(Path p) {
        return list(p.toFile().list());
    }
}

