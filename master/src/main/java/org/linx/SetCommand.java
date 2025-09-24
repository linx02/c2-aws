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

    private final SsmConfig cfgLoader;
    private final LambdaClient lambda;

    public SetCommand(SsmConfig cfgLoader, LambdaClient lambda) {
        this.cfgLoader = cfgLoader;
        this.lambda = lambda;
    }

    @Override
    public void run(List<String> args) {
        if (args.size() != 1) throw new InvalidUsageException(getUsage());

        Map<String, String> cfg;
        try (SsmConfig sc = this.cfgLoader) {
            cfg = sc.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String lambdaName = cfg.get("control_lambda");
        if (lambdaName == null || lambdaName.isBlank()) throw new IllegalArgumentException("Missing control_lambda in SSM");

        String command = args.getFirst();
        String payload = String.format("{\"command\": \"%s\"}", command);

        InvokeResponse resp = lambda.invoke(InvokeRequest.builder()
                .functionName(lambdaName)
                .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                .build());

        String out = resp.payload() == null ? "" : resp.payload().asUtf8String();
        if (resp.functionError() != null && !resp.functionError().isBlank()) {
            throw new RuntimeException("Lambda error: " + resp.functionError() + " | payload=" + out);
        }

        System.out.println("Command set successfully: " + command);
    }

    @Override public String getUsage() { return "set <command>"; }
}