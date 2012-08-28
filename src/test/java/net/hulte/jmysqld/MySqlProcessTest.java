package net.hulte.jmysqld;

import static net.hulte.jmysqld.MySqlProcess.*;
import static net.hulte.jmysqld.Utilities.*;
import static org.junit.Assert.*;
import static org.junit.rules.ExpectedException.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.*;
import java.nio.file.*;

import org.junit.*;
import org.junit.rules.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(JUnit4.class)
public class MySqlProcessTest {

    @Rule
    public ExpectedException thrown = none();


    @Test
    public void exception_is_thrown_if_process_cannot_be_started() {
        thrown.expect(MySqlProcessException.class);
        thrown.expectMessage("src/test/resources/unknown-script");

        startMySqlProcess(new ProcessBuilder(stubMysqldPath()
            .resolveSibling("unknown-script").toString()));
    }

    @Test
    public void exception_is_thrown_if_process_fails_while_waiting_for_successful_completion() {
        thrown.expect(MySqlProcessException.class);
        thrown.expectMessage("src/test/resources/stub-mysqld");
        thrown.expectMessage("exit-code: 99");
        thrown.expectMessage("error: instructed to fail with code 99"); // printed on stderr by the script

        startMySqlProcess(new ProcessBuilder(stubMysqld(), "failWithCode99"))
            .waitForSuccessfulCompletion();
    }

    @Test
    public void stdout_can_be_read_after_process_has_completed() {
        final MySqlProcess p = startMySqlProcess(new ProcessBuilder(stubMysqld()));
        p.waitForCompletion();

        assertThat(p.readStdOut(),
            equalTo("some output")); // printed on stdout by the script
    }


    static String stubMysqld() {
        return stubMysqldPath().toString();
    }

    static Path stubMysqldPath() {
        return path("src", "test", "resources", "stub-mysqld");
    }
}

