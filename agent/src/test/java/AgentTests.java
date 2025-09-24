import org.junit.jupiter.api.Test;
import org.linx.Main;
import org.linx.SeenStore;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class AgentTests {

    @Test
    void parseTxt_ok() {
        // Arrange
        String txt = "c0efb324-0081-455e-b70d-6af5fe6d9dba::echo pwned";

        // Act
        Main.ParsedCommand pc = Main.parseTxt(txt);

        // Assert
        assertEquals("c0efb324-0081-455e-b70d-6af5fe6d9dba", pc.id());
        assertEquals("echo pwned", pc.cmd());
    }

    @Test
    void seenStore_preventsDuplicate() throws Exception {
        // Arrange
        File tempFile = File.createTempFile("seen-test", ".txt");
        tempFile.deleteOnExit();
        SeenStore store = new SeenStore(tempFile);

        // Act
        boolean first = store.firstTime("c0efb324-0081-455e-b70d-6af5fe6d9dba");
        boolean second = store.firstTime("c0efb324-0081-455e-b70d-6af5fe6d9dba");

        // Assert
        assertTrue(first);
        assertFalse(second);
    }

}
