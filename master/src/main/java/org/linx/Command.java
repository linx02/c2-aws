package org.linx;

import java.util.List;

public interface Command {
    void run(List<String> args);
    String getUsage();
}
