package jason.mind.api.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebExchangeTest {

    private static final String STATE = "\"49000:beliefs,events,intentions\"";

    @Test
    void etagWithCommasMatchesItself() {
        assertTrue(WebExchange.matches(STATE, STATE));
    }

    @Test
    void weakTagAndListsMatch() {
        assertTrue(WebExchange.matches("W/" + STATE, STATE));
        assertTrue(WebExchange.matches("\"1:beliefs\", " + STATE, STATE));
        assertTrue(WebExchange.matches("*", STATE));
    }

    @Test
    void otherCycleOrPartsDoNotMatch() {
        assertFalse(WebExchange.matches("\"49001:beliefs,events,intentions\"", STATE));
        assertFalse(WebExchange.matches("\"49000:beliefs\"", STATE));
        assertFalse(WebExchange.matches("49000:beliefs,events,intentions", STATE));
    }
}
