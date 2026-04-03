package de.itsourcerer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @Test
    void testMain() {
        assertTrue(true);
    }


    @Test
        void shouldPrintHelloMessageWhenMainExecuted() {
            String[] args = {};
            Main.main(args);
            assertEquals("Hello from Main" + System.lineSeparator(), outContent.toString());
        }

    @Test
        void shouldNotThrowExceptionWhenArgsIsNull() {
            assertDoesNotThrow(() -> Main.main(null));
            // Even with null, it should still print the message
            assertEquals("Hello from Main" + System.lineSeparator(), outContent.toString());
        }
}