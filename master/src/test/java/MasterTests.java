import org.junit.jupiter.api.Test;
import org.linx.GetLog;
import org.linx.ListLogs;
import org.linx.SsmConfig;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linx.SetCommand;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MasterTests {

    @Mock S3Client s3;
    @Mock LambdaClient lambda;

    static class FakeSsm extends SsmConfig {
        private final Map<String,String> data;
        FakeSsm(Map<String,String> d){ this.data=d; }
        @Override public Map<String, String> load(){ return data; }
        @Override public void close() {}
    }

    @Test
    void listLogs_ok() {
        // Arrange
        var cfg = Map.of("log_bucket","c2-logs-demo","log_prefix","logs/");
        var cmd = new ListLogs(new FakeSsm(cfg), s3);

        var resp = ListObjectsV2Response.builder()
                .contents(
                        S3Object.builder().key("logs/a.txt").lastModified(Instant.parse("2025-09-23T10:00:00Z")).size(12L).build(),
                        S3Object.builder().key("logs/b.txt").lastModified(Instant.parse("2025-09-23T11:00:00Z")).size(34L).build()
                )
                .isTruncated(false)
                .build();
        when(s3.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(resp);

        // Act
        cmd.run(List.of());

        // Assert
        verify(s3, times(1)).listObjectsV2(argThat((ListObjectsV2Request r) ->
                r.bucket().equals("c2-logs-demo") && "logs/".equals(r.prefix())));
    }

    @Test
    void getLog_ok() {
        // Arrange
        var cfg = Map.of("log_bucket","c2-logs-demo","log_prefix","logs/");
        var cmd = new GetLog(new FakeSsm(cfg), s3);

        var listing = ListObjectsV2Response.builder()
                .contents(
                        S3Object.builder().key("logs/old.txt").lastModified(Instant.parse("2025-09-23T10:00:00Z")).build(),
                        S3Object.builder().key("logs/new.txt").lastModified(Instant.parse("2025-09-23T12:00:00Z")).build()
                )
                .build();
        when(s3.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listing);

        byte[] body = """
                # id: abc
                # cmd: echo pwned

                pwned
                """.getBytes(StandardCharsets.UTF_8);
        ResponseInputStream<GetObjectResponse> objStream = new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength((long) body.length).build(),
                AbortableInputStream.create(new ByteArrayInputStream(body))
        );
        when(s3.getObject(any(GetObjectRequest.class))).thenReturn(objStream);

        // Act
        cmd.run(List.of("latest"));

        // Assert
        verify(s3).getObject(argThat((GetObjectRequest r) ->
                r.bucket().equals("c2-logs-demo") && r.key().equals("logs/new.txt")));
    }

    @Test
    void setCommand_ok() {
        // Arrange
        var cfg = Map.of("control_lambda","c2-control-dev");
        var cmd = new SetCommand(new FakeSsm(cfg), lambda);

        when(lambda.invoke(any(InvokeRequest.class)))
                .thenReturn(InvokeResponse.builder()
                        .statusCode(200)
                        .functionError((String) null)
                        .payload(SdkBytes.fromUtf8String("{}"))
                        .build());

        // Act
        cmd.run(List.of("echo 'pwned'"));

        // Assert
        var reqCap = ArgumentCaptor.forClass(InvokeRequest.class);
        verify(lambda, times(1)).invoke(reqCap.capture());
        var req = reqCap.getValue();
        assertEquals("c2-control-dev", req.functionName());
        String json = req.payload().asUtf8String();
        assertTrue(json.contains("\"command\""));
        assertTrue(json.contains("echo 'pwned'"));
    }
}