package org.linx;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

public class Main {

    private static final String env = "prod";

    public static void main(String[] args) throws Exception {
        Map<String,String> cfg;
        try (SsmConfig sc = new SsmConfig()) {
            cfg = sc.load();
        }

        String recordName = cfg.get("record_name");
        String bucket     = cfg.get("log_bucket");
        String prefix     = cfg.get("log_prefix");
        String pollMsStr  = cfg.get("poll_ms");
        long   pollMs     = Long.parseLong(cfg.get("poll_ms"));

        if (bucket == null || bucket.isBlank())
            throw new IllegalArgumentException("Missing log bucket");

        CommandResolver resolver;
        if ("prod".equals(env)) {
            // Riktig DNS
            resolver = new DnsJavaResolver(null);
        } else {
            // Kör via API medans vi utvecklar
            String zoneId = cfg.get("zone_id");
            if (zoneId == null) throw new IllegalArgumentException("Missing zone_id");
            resolver = new Route53ApiResolver(zoneId);
        }

        S3Client s3 = S3Client.builder()
                .region(Region.of( "eu-north-1"))
                .build();

        // Spara körda id:n för att undvika dubbletter
        SeenStore seen = new SeenStore(new File("seen-ids.txt"));
        CommandExecutor exec = new ProcessExecutor();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("Goodbye!")));

        while (true) {
            try {
                if ("dev".equals(env)) {
                    System.out.println("Polling for commands...");
                }
                runOnce(resolver, recordName, exec, s3, bucket, prefix);
            } catch (Exception e) {
                System.err.println("Poll error: " + e.getMessage());
            }
            Thread.sleep(pollMs);
        }
    }

    static void runOnce(CommandResolver resolver, String recordName,
                        CommandExecutor exec, S3Client s3, String bucket, String prefix) throws Exception {
        String txt = resolver.fetch(recordName);
        if (txt == null || txt.isBlank()) return;
        if ("dev".equals(env)) {
            System.out.println("Fetched TXT: " + txt);
        }

        ParsedCommand parsed = parseTxt(txt);

        if (!Singletons.seen().firstTime(parsed.id)) return;

        CommandExecutor.Result res = exec.exec(parsed.cmd);

        if ("dev".equals(env)) {
            System.out.println("--- command output ---");
            System.out.print(res.output());
            System.out.println("----------------------");
        }

        String body = """
            # time: %s
            # id:   %s
            # cmd:  %s
            # exit: %d

            %s
            """.formatted(Instant.now(), parsed.id, parsed.cmd, res.exitCode(), res.output());

        s3.putObject(
                PutObjectRequest.builder().bucket(bucket).key(prefix + parsed.id + ".txt").build(),
                RequestBody.fromBytes(body.getBytes(StandardCharsets.UTF_8))
        );

        if ("dev".equals(env)) {
            System.out.printf("Executed id=%s exit=%d%n", parsed.id, res.exitCode());
        }
    }

    static class Singletons {
        private static SeenStore SEEN;
        static SeenStore seen() {
            if (SEEN == null) SEEN = new SeenStore(new File("seen-ids.txt"));
            return SEEN;
        }
    }

    public static ParsedCommand parseTxt(String txt) {
        int sep = txt.indexOf("::");
        if (sep <= 0 || sep == txt.length() - 2) {
            throw new IllegalArgumentException("Invalid TXT record: " + txt);
        }
        String id  = txt.substring(0, sep);
        String cmd = txt.substring(sep + 2);
        return new ParsedCommand(id, cmd);
    }

    public record ParsedCommand(String id, String cmd) {}

}