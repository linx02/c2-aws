package org.linx;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GetLog implements Command {

    private final SsmConfig cfgLoader;
    private final S3Client s3;

    public GetLog(SsmConfig cfgLoader, S3Client s3) {
        this.cfgLoader = cfgLoader;
        this.s3 = s3;
    }

    @Override
    public void run(List<String> args) {
        if (args.size() != 1 || !(args.getFirst().equals("latest") || isValidUUID(args.getFirst()))) {
            throw new InvalidUsageException(getUsage());
        }

        Map<String, String> cfg;
        try (SsmConfig sc = this.cfgLoader) {
            cfg = sc.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String bucket = cfg.get("log_bucket");
        String prefix = cfg.get("log_prefix");
        if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException("Missing log_bucket in SSM");

        String which = args.getFirst();
        String key;
        if ("latest".equalsIgnoreCase(which)) {
            var res = s3.listObjectsV2(
                    ListObjectsV2Request.builder()
                            .bucket(bucket)
                            .prefix(prefix)
                            .build()
            );
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

    @Override public String getUsage() { return "get <uuid|latest>"; }

    private boolean isValidUUID(String uuid) {
        return uuid != null && uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
}