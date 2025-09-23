package org.linx;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class SetCommand implements Command {
    public void run(List<String> args) {
        if (args.size() != 1) {
            throw new InvalidUsageException(getUsage());
        }

        String ssmPrefix = "/c2";
        Map<String, String> cfg;
        try (SsmConfig sc = new SsmConfig()) {
            cfg = sc.load();
        }

        String lambdaName = cfg.get("control_lambda");
        if (lambdaName == null || lambdaName.isBlank()) {
            throw new IllegalArgumentException("Missing control_lambda in SSM");
        }
        Region region = Region.of("eu-north-1");

        String command = args.getFirst();
        String payload = String.format("{\"command\": \"%s\"}", command);

        try (LambdaClient client = LambdaClient.builder().region(region).build()) {
            InvokeResponse resp = client.invoke(InvokeRequest.builder()
                    .functionName(lambdaName)
                    .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                    .build());

            String out = resp.payload() == null ? "" : resp.payload().asUtf8String();

            // Kasta error om något gick fel
            if (resp.functionError() != null && !resp.functionError().isBlank()) {
                throw new RuntimeException("Lambda error: " + resp.functionError() + " | payload=" + out);
            }

            System.out.println("Command set successfully: " + command);
        }
    }

    public String getUsage() {
        return "set <command>";
    }
}
