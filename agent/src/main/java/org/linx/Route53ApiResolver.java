package org.linx;

import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

import java.util.List;
import java.util.stream.Collectors;

public class Route53ApiResolver implements CommandResolver {
    private final Route53Client r53 = Route53Client.create();
    private final String zoneId;

    public Route53ApiResolver(String zoneId) { this.zoneId = zoneId; }

    @Override
    public String fetch(String fqdn) {
        String name = fqdn.endsWith(".") ? fqdn : fqdn + ".";

        ListResourceRecordSetsResponse resp = r53.listResourceRecordSets(
                ListResourceRecordSetsRequest.builder()
                        .hostedZoneId(zoneId)
                        .startRecordName(name)
                        .startRecordType(RRType.TXT)
                        .maxItems("1")
                        .build()
        );

        List<ResourceRecordSet> sets = resp.resourceRecordSets();
        if (sets == null || sets.isEmpty()) return null;

        ResourceRecordSet rrset = sets.getFirst();
        if (!name.equals(rrset.name()) || rrset.type() != RRType.TXT) return null;
        if (rrset.resourceRecords() == null || rrset.resourceRecords().isEmpty()) return null;

        return rrset.resourceRecords().stream()
                .map(ResourceRecord::value)
                .map(Route53ApiResolver::strip)
                .collect(Collectors.joining());
    }

    private static String strip(String s) {
        if (s == null || s.length() < 2) return s;
        return (s.charAt(0) == '"' && s.charAt(s.length()-1) == '"')
                ? s.substring(1, s.length()-1) : s;
    }

    @Override public void close() { r53.close(); }
}