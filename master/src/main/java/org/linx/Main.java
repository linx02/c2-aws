package org.linx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {

    private static final Map<String, Class<? extends Command>> COMMANDS = Map.of(
            "set", SetCommand.class,
            "get", GetLog.class,
            "list", ListLogs.class
    );

    private static final String banner = """
                                                                                                    \s
         @@@@@@@   @@@@@@      @@@@@@@@@@    @@@@@@    @@@@@@   @@@@@@@  @@@@@@@@  @@@@@@@  \s
        @@@@@@@@  @@@@@@@@     @@@@@@@@@@@  @@@@@@@@  @@@@@@@   @@@@@@@  @@@@@@@@  @@@@@@@@ \s
        !@@            @@@     @@! @@! @@!  @@!  @@@  !@@         @@!    @@!       @@!  @@@ \s
        !@!           @!@      !@! !@! !@!  !@!  @!@  !@!         !@!    !@!       !@!  @!@ \s
        !@!          !!@       @!! !!@ @!@  @!@!@!@!  !!@@!!      @!!    @!!!:!    @!@!!@!  \s
        !!!         !!:        !@!   ! !@!  !!!@!!!!   !!@!!!     !!!    !!!!!:    !!@!@!   \s
        :!!        !:!         !!:     !!:  !!:  !!!       !:!    !!:    !!:       !!: :!!  \s
        :!:       :!:          :!:     :!:  :!:  !:!      !:!     :!:    :!:       :!:  !:! \s
         ::: :::  :: :::::     :::     ::   ::   :::  :::: ::      ::     :: ::::  ::   ::: \s
         :: :: :  :: : :::      :      :     :   : :  :: : :       :     : :: ::    :   : : \s
        """;

    public static void main(String[] args) {
        System.out.println(banner);

        while (true) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.print("master> ");
                String command = reader.readLine();

                List<String> cmdList = tokenize(command);

                if (cmdList.isEmpty() || cmdList.getFirst().isBlank()) {
                    continue;
                }

                if (cmdList.getFirst().equals("exit")) {
                    System.out.println("Goodbye!");
                    break;
                }

                if (!COMMANDS.containsKey(cmdList.getFirst())) {
                    System.out.println("Unknown command: " + command);
                    continue;
                }

                Command cmd = COMMANDS.get(cmdList.getFirst()).getDeclaredConstructor().newInstance();
                cmd.run(cmdList.subList(1, cmdList.size()));
            }
            catch (InvalidUsageException e) {
                System.out.println("Usage: " + e.getMessage());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Behövs i fallet av t.ex: set "echo 'hello world'"
    public static List<String> tokenize(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (!inQuotes && Character.isWhitespace(c)) {
                if (!cur.isEmpty()) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        if (!cur.isEmpty()) out.add(cur.toString());
        return out;
    }
}