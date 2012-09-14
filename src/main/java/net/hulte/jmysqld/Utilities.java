package net.hulte.jmysqld;

import static java.util.Arrays.asList;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
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
     * Starts a named, daemon thread.
     */
    static void startNamedDaemon(String name, Runnable daemon) {
        final Thread t = new Thread(daemon, name);
        t.setDaemon(true);
        t.start();
    }

    static String userName() {
        return System.getProperty("user.name");
    }

    static Path tmpDir() {
        return new File(System.getProperty("java.io.tmpdir")).toPath();
    }

    static ProcessBuilder newProcessBuilder(Path command, String... args) {
        return newProcessBuilder(command, list(args));
    }

    static ProcessBuilder newProcessBuilder(Path command, Collection<String> args) {
        final List<String> commandAndArgs = list(command.toString());
        commandAndArgs.addAll(args);
        return new ProcessBuilder(commandAndArgs);
    }

    static BufferedReader asReader(InputStream in) {
        return new BufferedReader(new InputStreamReader(in));
    }

    static <E> List<E> list() {
        return new ArrayList<>();
    }

    static <E> List<E> list(E... elements) {
        return new ArrayList<>(asList(elements));
    }

    static <E> Set<E> set() {
        return new HashSet<>();
    }

    static Path path(String first, String... more) {
        return FileSystems.getDefault().getPath(first, more);
    }

    static void addShutdownHook(Runnable hook) {
        Runtime.getRuntime().addShutdownHook(new Thread(hook));
    }

    static void sleep(final long millis) {
        execute(new Interruptible() {
            @Override public void run() throws InterruptedException {
                Thread.sleep(millis);
            }
        });
    }

    static void await(final CountDownLatch latch) {
        execute(new Interruptible() {
            @Override public void run() throws InterruptedException {
                latch.await();
            }
        });
    }

    static void execute(Interruptible task) {
        try {
            task.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public static interface Interruptible {
        void run() throws InterruptedException;
    }

    private Utilities() {}
}

