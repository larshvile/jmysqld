package net.hulte.jmysqld;

import static net.hulte.jmysqld.Utilities.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.*;
import java.util.regex.*;

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(JUnit4.class)
public class UtilitiesTest {

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
}

