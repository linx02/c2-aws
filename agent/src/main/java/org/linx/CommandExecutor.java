package org.linx;

public interface CommandExecutor {
    record Result(int exitCode, String output) {}
    Result exec(String command) throws Exception;
}
