package net.hulte.jmysqld;

import static net.hulte.jmysqld.Utilities.*;
import static org.junit.Assert.*;
import static org.junit.rules.ExpectedException.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.*;
import java.nio.file.*;
import java.util.regex.*;

import org.junit.*;
import org.junit.rules.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(JUnit4.class)
public class UtilitiesTest {

    @Rule
    public ExpectedException thrown = none();


    @Test
    public void text_is_read_from_input_stream() throws IOException {
        assertThat(readText(new ByteArrayInputStream("123".getBytes())),
            equalTo("123"));
    }

    @Test
    public void text_read_from_stream_is_trimmed() throws IOException {
        assertThat(readText(new ByteArrayInputStream("  123\n".getBytes())),
            equalTo("123"));
    }

    @Test
    public void pattern_matching_works() {
        final Pattern number = Pattern.compile(".*(\\d).*");
        assertThat(firstMatchOrNull(number, "abc"), equalTo(null));
        assertThat(firstMatchOrNull(number, "ab4c"), equalTo("4"));
    }

    @Test
    public void output_is_collected_from_process() {
        assertThat(collectOutput(new ProcessBuilder(stubMysqld())),
            equalTo("some output"));
    }

    @Test
    public void exception_is_thrown_if_process_fails_while_collecting_output() {
        thrown.expect(MySqlProcessException.class);
        thrown.expectMessage("src/test/resources/stub-mysqld");
        thrown.expectMessage("exit-code: 99");
        thrown.expectMessage("error: instructed to fail with code 99"); // printed on stderr by the script

        collectOutput(new ProcessBuilder(stubMysqld(), "failWithCode99"));
    }

    @Test
    public void exception_is_thrown_if_process_cannot_be_started() {
        thrown.expect(MySqlProcessException.class);
        thrown.expectMessage("src/test/resources/unknown-script");

        collectOutput(new ProcessBuilder(stubMysqldPath()
            .resolveSibling("unknown-script").toString()));
    }


    static String stubMysqld() {
        return stubMysqldPath().toString();
    }

    static Path stubMysqldPath() {
        return path("src", "test", "resources", "stub-mysqld");
    }
}

