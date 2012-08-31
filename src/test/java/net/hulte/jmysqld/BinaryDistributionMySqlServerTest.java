package net.hulte.jmysqld;

import static net.hulte.jmysqld.MySql.*;
import static net.hulte.jmysqld.Utilities.*;
import static org.junit.Assert.*;
import static org.junit.rules.ExpectedException.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.junit.*;
import org.junit.rules.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(JUnit4.class)
public class BinaryDistributionMySqlServerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = none();


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
        assertThat(contents(dataDir()), not(hasItem("mysql")));

        theServer().initializeDataDirectory(dataDir());

        assertThat(contents(dataDir()), hasItem("mysql"));
    }

    @Test
    public void server_can_be_started_and_stopped_via_datadir() {
        final MySqlServer server = theServer();

        assertFalse(server.isInstanceRunningIn(dataDir()));

        server.initializeDataDirectory(dataDir()); // TODO helper?
        server.start(dataDir()); // TODO AUTO_SHUTDOWN

        assertTrue(server.isInstanceRunningIn(dataDir()));

        server.shutdownInstanceIn(dataDir());

        assertFalse(server.isInstanceRunningIn(dataDir()));
    }

    @Test
    public void server_can_be_started_and_stopped_via_instance() {
        final MySqlServer server = theServer();

        assertFalse(server.isInstanceRunningIn(dataDir()));

        server.initializeDataDirectory(dataDir()); // TODO helper?
        final MySqlServerInstance instance = server.start(dataDir()); // TODO AUTO_SHUTDOWN

        assertTrue(instance.isRunning());
        assertTrue(server.isInstanceRunningIn(dataDir()));

        instance.shutdown();

        assertFalse(instance.isRunning());
        assertFalse(server.isInstanceRunningIn(dataDir()));
    }

    @Test
    public void exception_is_thrown_if_server_fails_to_start() {
        thrown.expect(MySqlProcessException.class);
        thrown.expectMessage("Failed to start");
        thrown.expectMessage("see " + dataDir().resolve("error.log"));

        // starting the server with an uninitialized data directory should guarantee failure..
        theServer().start(dataDir());
    }

    // TODO test the SHUTDOWN_EXISTING option by firing up two instances in the same dataDir

    // TODO make sure that thing works by connecting with jdbc & having some fun...


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

