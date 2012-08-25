package net.hulte.jmysqld;

import static java.nio.file.Files.exists;
import static java.util.Arrays.asList;

import java.io.*;
import java.nio.file.*;
import java.util.*;

final class BinaryDistributionMySqlServer implements MySqlServer {

    final Path distPath;

    BinaryDistributionMySqlServer(Path distPath) {
        this.distPath = distPath.toAbsolutePath();
        if (!exists(mysqldPath())) {
            throw new IllegalArgumentException("mysqld binary not found at at " + mysqldPath() + ".");
        }
    }

    @Override
    public String getVersion() {
        final String versionOutput = collectOutput("--version");
        return versionOutput; // TODO filter it
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + distPath;
    }

    private Path mysqldPath() {
        return distPath.resolve("bin").resolve("mysqld");
    }
    
    private String collectOutput(String... args) {
        final ProcessBuilder pb = mysqldProcessBuilder(args);
        
        try {
            final Process p = pb.start();
            p.waitFor();
            
            if (p.exitValue() != 0) { // TODO custom exception please..
                throw new RuntimeException("mysqld exited with code "
                    + p.exitValue() + ", error: "
                    + readText(p.getErrorStream()));
            }

            return readText(p.getInputStream());            
        } catch (Exception e) {
            throw new RuntimeException(e); // TODO get some control on this        
        }
    }
    
    private ProcessBuilder mysqldProcessBuilder(String... args) {
        final List<String> allArgs = new ArrayList<>();
        allArgs.add(mysqldPath().toString()); // TODO really?
        allArgs.addAll(asList(args));
        return new ProcessBuilder(allArgs);
    }
    
    private static String readText(InputStream in) throws IOException { // TODO fix me =)
        final BufferedReader r = asReader(in);
        
        String result = "";
        while (true) {
            final String line = r.readLine();
            if (line == null) {
                break;
            }
            if (!result.isEmpty()) {
                result += "\n";
            }
            result += line;
        }
    
        return result;
    }
    
    private static BufferedReader asReader(InputStream in) {
        return new BufferedReader(new InputStreamReader(in));
    }
}

