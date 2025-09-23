package org.linx;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GetLog implements Command {
    public void run(List<String> args) {
        if (args.size() != 1 || !(args.getFirst().equals("latest") || isValidUUID(args.getFirst()))) {
            throw new InvalidUsageException(getUsage());
        }

        Map<String, String> cfg;
        try (SsmConfig sc = new SsmConfig()) {
            cfg = sc.load();
        }
        String bucket = cfg.get("log_bucket");
        String prefix = cfg.get("log_prefix");

        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Missing log_bucket in SSM");
        }

        String which = args.getFirst();

        try (S3Client s3 = S3Client.builder().region(Region.of("eu-north-1")).build()) {
            String key;
            if ("latest".equalsIgnoreCase(which)) {
                var res = s3.listObjectsV2(b -> b.bucket(bucket).prefix(prefix));
                if (res.contents()==null || res.contents().isEmpty()) { System.out.println("No logs found."); return; }
                var latest = res.contents().stream().max(Comparator.comparing(S3Object::lastModified)).orElseThrow();
                key = latest.key();
            } else {
                key = prefix + (which.endsWith(".txt") ? which : which + ".txt");
            }

            try (ResponseInputStream<GetObjectResponse> in =
                         s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
                 BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

                System.out.println("# " + key);
                br.lines().forEach(System.out::println);
            } catch (NoSuchKeyException e) {
                System.out.println("No such log: " + key);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getUsage() {
        return "get <uuid|latest>";
    }

    private boolean isValidUUID(String uuid) {
        return uuid != null && uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
}
