package org.linx;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;


public class DnsJavaResolver implements CommandResolver {
    private final Resolver resolver;

    public DnsJavaResolver(String resolverIp) throws Exception {
        this.resolver = (resolverIp == null || resolverIp.isBlank()) ? null : new SimpleResolver(resolverIp);
    }

    @Override public String fetch(String fqdn) throws Exception {
        Lookup lookup = new Lookup(Name.fromString(fqdn, Name.root), Type.TXT);
        if (resolver != null) lookup.setResolver(resolver);
        Record[] ans = lookup.run();
        if (ans == null || ans.length == 0) return null;
        TXTRecord txt = (TXTRecord) ans[0];
        return String.join("", txt.getStrings());
    }

    @Override public void close() {  }
}
