package org.linx;

import java.util.List;

public class GetLog implements Command {
    public void run(List<String> args) {
        if (args.size() != 1 || !(args.getFirst().equals("latest") || isValidUUID(args.getFirst()))) {
            throw new InvalidUsageException(getUsage());
        }

        if (args.getFirst().equals("latest")) {
            System.out.println("Getting latest log...");
        } else {
            System.out.println("Getting log with UUID: " + args.getFirst());
        }
        System.out.println("Log content here...");
    }

    public String getUsage() {
        return "get <uuid|latest>";
    }

    private boolean isValidUUID(String uuid) {
        return uuid != null && uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
}
