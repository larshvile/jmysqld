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

        startMySqlProcess(newProcessBuilder(stubMysqld()
            .resolveSibling("unknown-script")));
    }

    @Test
    public void stdout_can_be_read_after_process_has_completed() {
        final MySqlProcess p = startMySqlProcess(newProcessBuilder(stubMysqld()))
            .waitForCompletion();

        assertThat(p.readStdOut(),
            equalTo("some output")); // printed on stdout by the script
    }

    @Test
    public void stderr_can_be_read_after_process_has_completed() {
        final MySqlProcess p = startMySqlProcess(newProcessBuilder(stubMysqld(), "failWithCode99"))
            .waitForCompletion();

        assertThat(p.readStdErr(),
            equalTo("instructed to fail with code 99")); // printed on stdout by the script
    }

    @Test
    public void exception_is_thrown_if_process_fails_while_waiting_for_successful_completion() {
        thrown.expect(MySqlProcessException.class);
        thrown.expectMessage("src/test/resources/stub-mysqld");
        thrown.expectMessage("exit-code: 99");
        thrown.expectMessage("error: instructed to fail with code 99");

        startMySqlProcess(newProcessBuilder(stubMysqld(), "failWithCode99"))
            .waitForSuccessfulCompletion();
    }

    @Test
    public void exit_code_can_be_retrieved() {
        assertThat(startMySqlProcess(newProcessBuilder(stubMysqld()))
                .waitForCompletion()
                .exitCode(),
            equalTo(0));

        assertThat(startMySqlProcess(newProcessBuilder(stubMysqld(), "failWithCode99"))
                .waitForCompletion()
                .exitCode(),
            equalTo(99));
    }

    static Path stubMysqld() {
        return path("src", "test", "resources", "stub-mysqld");
    }
}

