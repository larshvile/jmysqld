package net.hulte.jmysqld;

import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.UUID.randomUUID;
import static net.hulte.jmysqld.MySql.*;
import static net.hulte.jmysqld.InstanceSpec.Option.*;
import static net.hulte.jmysqld.Utilities.*;
import static org.junit.Assert.*;
import static org.junit.rules.ExpectedException.none;
import static org.hamcrest.CoreMatchers.*;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

import org.junit.*;
import org.junit.rules.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(JUnit4.class)
public class BinaryDistributionMySqlServerTest {

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
    public void empty_folder_is_initialized_with_mysql_data_files() {
        final Path dataDir = newDataDir();

        assertThat(contents(dataDir), not(hasItem("mysql")));
        theServer().initializeDataDirectory(dataDir);
        assertThat(contents(dataDir), hasItem("mysql"));
    }

    @Test
    public void server_can_be_started_and_stopped_via_datadir() {
        final MySqlServer server = theServer();
        final Path dataDir = newPreparedDataDir();

        assertFalse(server.isInstanceRunningIn(dataDir));

        server.start(dataDir, defaultSpec());

        assertTrue(server.isInstanceRunningIn(dataDir));

        server.shutdownInstanceIn(dataDir);

        assertFalse(server.isInstanceRunningIn(dataDir));
    }

    @Test
    public void server_can_be_started_and_stopped_via_instance() {
        final MySqlServer server = theServer();
        final Path dataDir = newPreparedDataDir();

        assertFalse(server.isInstanceRunningIn(dataDir));

        final MySqlServerInstance instance = server.start(dataDir, defaultSpec());

        assertTrue(instance.isRunning());
        assertTrue(server.isInstanceRunningIn(dataDir));

        instance.shutdown();

        assertFalse(instance.isRunning());
        assertFalse(server.isInstanceRunningIn(dataDir));
    }

    @Test
    public void exception_is_thrown_if_server_fails_to_start() {
        final Path dataDir = newDataDir();

        thrown.expect(MySqlProcessException.class);
        thrown.expectMessage("Failed to start");

        // starting the server with an uninitialized data directory should guarantee failure..
        theServer().start(dataDir, defaultSpec());
    }

    @Test
    public void the_second_instance_starting_in_the_same_data_directory_fails_to_start() {
        final MySqlServer server = theServer();
        final Path dataDir = newPreparedDataDir();

        final MySqlServerInstance i1 = server.start(dataDir, defaultSpec());

        try {
            server.start(dataDir, defaultSpec());
            fail();
        } catch (MySqlProcessException e) {}

        assertTrue(i1.isRunning());
        i1.shutdown();
    }

    @Test
    public void instance_started_with_specific_port_can_be_connected_to_with_jdbc() throws Exception {
        final Path dataDir = newPreparedDataDir();
        final MySqlServerInstance i = theServer().start(dataDir,
                defaultSpec().port(mysqlPort()));

        final ResultSet res = query("select 123");
        assertTrue(res.next());
        assertThat(res.getInt(1), equalTo(123));

        i.shutdown();
    }

    @Test
    public void instance_started_with_defaults_file_can_be_connected_to_with_jdbc() throws Exception {
        final Path dataDir = newPreparedDataDir();
        final Path defaultsFile = dataDir.resolve("settings.cfg");

        try (PrintWriter w = new PrintWriter(newBufferedWriter(defaultsFile, defaultCharset()))) {
            w.println("[mysqld_safe]");
            w.println("port=" + mysqlPort());
        }

        final MySqlServerInstance i = theServer().start(dataDir,
                defaultSpec().defaultsFile(defaultsFile));

        final ResultSet res = query("select 'abc'");
        assertTrue(res.next());
        assertThat(res.getString(1), equalTo("abc"));

        i.shutdown();
    }


    InstanceSpec defaultSpec() {
        return new InstanceSpec().option(AUTO_SHUTDOWN);
    }

    Path newPreparedDataDir() {
        final Path result = newDataDir();
        theServer().initializeDataDirectory(result);
        return result;
    }

    Path newDataDir() {
        return path("target", "mysql-data", randomUUID().toString())
            .toAbsolutePath();
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

    static Integer mysqlPort() {
        final String port = System.getProperty("mysqlPort");
        return port == null
            ? 3306
            : Integer.parseInt(port);
    }

    static ResultSet query(String query) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        final Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:" + mysqlPort());
        final Statement stmt = conn.createStatement();
        return stmt.executeQuery(query);
    }

    static List<String> contents(Path p) {
        final String[] contents = p.toFile().list();
        return contents == null
            ? Utilities.<String>list()
            : list(contents);
    }
}

