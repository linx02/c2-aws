package org.linx;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.HashMap;
import java.util.Map;

public class SsmConfig implements AutoCloseable {
    private final SsmClient ssm = SsmClient.create();

    public Map<String,String> load() {
        Map<String,String> out = new HashMap<>();
        String token = null;
        do {
            String prefix = "/c2";
            var resp = ssm.getParametersByPath(GetParametersByPathRequest.builder()
                    .path(prefix)
                    .recursive(false)
                    .withDecryption(true)
                    .maxResults(10)
                    .nextToken(token)
                    .build());
            for (Parameter p : resp.parameters()) {
                String key = p.name().substring(prefix.length() + 1);
                out.put(key, p.value());
            }
            token = resp.nextToken();
        } while (token != null && !token.isBlank());
        return out;
    }

    @Override public void close() { ssm.close(); }
}
