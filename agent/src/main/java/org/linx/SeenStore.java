package org.linx;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class SeenStore {
    private final Set<String> seen = new HashSet<>();
    private final File file;

    public SeenStore(File file) {
        this.file = file;
        load();
    }

    public synchronized boolean firstTime(String id) {
        if (seen.contains(id)) return false;
        seen.add(id);
        persist(id);
        return true;
    }

    private void load() {
        if (file == null || !file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            for (String line; (line = br.readLine()) != null; ) {
                if (!line.isBlank()) seen.add(line.trim());
            }
        } catch (IOException ignored) {}
    }

    private void persist(String id) {
        if (file == null) return;
        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8, true)) {
            fw.write(id); fw.write('\n');
        } catch (IOException ignored) {}
    }
}
