package jason.mind.api.infrastructure.adapters.in.jason.capture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageDirectoryTest {

    @Test
    void findsTheEntityOfAMessageAndItsReceiver() {
        MessageDirectory d = new MessageDirectory();
        d.put("m1", "bob", 10);
        d.put("m1", "carol", 11);
        assertEquals(10L, d.get("m1", "bob"));
        assertEquals(11L, d.get("m1", "carol"));
        assertNull(d.get("m1", "dave"));
    }

    @Test
    void forgettingAnEntityDropsOnlyItsEntry() {
        MessageDirectory d = new MessageDirectory();
        d.put("m1", "bob", 10);
        d.put("m1", "carol", 11);
        d.forget(10);
        assertNull(d.get("m1", "bob"));
        assertEquals(11L, d.get("m1", "carol"));
    }

    @Test
    void forgettingAnUnknownEntityDoesNothing() {
        MessageDirectory d = new MessageDirectory();
        d.put("m1", "bob", 10);
        d.forget(99);
        assertEquals(10L, d.get("m1", "bob"));
    }

    @Test
    void forgettingAReplacedEntityKeepsTheNewEntry() {
        MessageDirectory d = new MessageDirectory();
        d.put("m1", "bob", 10);
        d.put("m1", "bob", 12);
        d.forget(10);
        assertEquals(12L, d.get("m1", "bob"));
    }
}
