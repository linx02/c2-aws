package org.linx;

import java.util.List;

public class ListLogs implements Command {
    public void run(List<String> args) {
        System.out.println("ListLogs");
    }

    public String getUsage() {
        return "list";
    }
}
