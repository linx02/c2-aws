package org.linx;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ListLogs implements Command {
    public void run(List<String> args) {
        Map<String, String> cfg;
        try (SsmConfig sc = new SsmConfig()) {
            cfg = sc.load();
        }

        String bucket = cfg.get("log_bucket");
        String prefix = cfg.get("log_prefix");

        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Missing log_bucket in SSM");
        }

        try (S3Client s3 = S3Client.builder().region(Region.of("eu-north-1")).build()) {
            String token = null;
            do {
                var res = s3.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(bucket).prefix(prefix).continuationToken(token).build());
                if (res.contents()!=null) {
                    for (S3Object o : res.contents()) {
                        String ts = o.lastModified()==null? "-" : DateTimeFormatter.ISO_INSTANT.format(o.lastModified());
                        System.out.printf("%-40s %10d  %s%n", o.key().replace(".txt", "").replace("logs/", ""), o.size(), ts);
                    }
                }
                token = res.isTruncated() ? res.nextContinuationToken() : null;
            } while (token != null);
        }

    }

    public String getUsage() {
        return "list";
    }
}
