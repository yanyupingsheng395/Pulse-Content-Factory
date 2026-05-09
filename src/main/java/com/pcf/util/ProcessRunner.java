package com.pcf.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ProcessRunner {

    private ProcessRunner() {
    }

    public static Result run(List<String> command, long timeoutSeconds) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        byte[] out = readAllBytes(p.getInputStream());
        boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new IOException("Command timed out: " + command);
        }
        int code = p.exitValue();
        String output = new String(out, StandardCharsets.UTF_8);
        return new Result(code, output);
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) != -1) {
            buf.write(b, 0, n);
        }
        return buf.toByteArray();
    }

    public static final class Result {
        private final int exitCode;
        private final String output;

        public Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }

        public void requireSuccess() throws IOException {
            if (exitCode != 0) {
                throw new IOException("exit=" + exitCode + " output=" + output);
            }
        }
    }

    public static List<String> shellSplit(String executable, String... args) {
        List<String> c = new ArrayList<>();
        c.add(executable);
        for (String a : args) {
            c.add(a);
        }
        return c;
    }
}
