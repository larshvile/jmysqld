package net.hulte.jmysqld;

import static java.util.Arrays.asList;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

final class Utilities {

    /**
     * Reads text from an input stream using the default charset. The resulting string is
     * trimmed before being returned.
     */
    static String readText(InputStream in) throws IOException {
        String result = "";

        final BufferedReader r = asReader(in);
        final char[] buffer = new char[100];

        while (true) {
            final int numRead = r.read(buffer, 0, buffer.length);
            if (numRead == -1) {
                break;
            }
            result += new String(buffer, 0, numRead);
        }

        return result.trim();
    }

    /**
     * Returns the first matching group, or {@code null} from a provided regex pattern
     * and input string.
     */
    static String firstMatchOrNull(Pattern pattern, String str) {
        final Matcher m = pattern.matcher(str);
        return m.matches() ? m.group(1) : null;
    }

    /**
     * Starts the MySQL process, waits for it to complete and returns its output.
     *
     * @param pb the MySQL process, in the form of a {@link ProcessBuilder}
     * @return the output printed on stdout
     * @throws MySqlProcessException on errors starting the process
     */
    static String collectOutput(ProcessBuilder pb) {
        try {
            final Process p = pb.start();
            p.waitFor();

            if (p.exitValue() != 0) {
                throw new MySqlProcessException("Failed to collect output from '"
                    + pb.command() + "', exit-code: " + p.exitValue()
                    + ", error: " + readText(p.getErrorStream()));
            }

            return readText(p.getInputStream());
        } catch (IOException|InterruptedException e) {
            throw new MySqlProcessException("Unable to run '" + pb.command() + "'.", e);
        }
    }

    static BufferedReader asReader(InputStream in) {
        return new BufferedReader(new InputStreamReader(in));
    }

    static <E> List<E> list() {
        return new ArrayList<E>();
    }

    static <E> List<E> list(E... elements) {
        return new ArrayList<E>(asList(elements));
    }

    static Path path(String first, String... more) {
        return FileSystems.getDefault().getPath(first, more);
    }

    private Utilities() {}
}

