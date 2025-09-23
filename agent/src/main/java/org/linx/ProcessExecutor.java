package org.linx;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ProcessExecutor implements CommandExecutor {
    @Override public Result exec(String command) throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        ProcessBuilder pb;
        if (os.contains("win")) {
            pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", command);
        } else {
            pb = new ProcessBuilder("/bin/sh", "-lc", command);
        }
        pb.redirectErrorStream(true);
        Process p = pb.start();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (InputStream in = p.getInputStream()) { in.transferTo(buf); }
        boolean ok = p.waitFor(60, TimeUnit.SECONDS);
        int ec = ok ? p.exitValue() : -1;
        if (!ok) p.destroyForcibly();
        return new Result(ec, buf.toString(StandardCharsets.UTF_8));
    }
}
