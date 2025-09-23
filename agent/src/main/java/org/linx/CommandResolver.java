package org.linx;

public interface CommandResolver extends AutoCloseable {
    String fetch(String fqdn) throws Exception;

    @Override
    void close() throws Exception;
}
