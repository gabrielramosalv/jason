package jason.mind.api.core.ports.in.capture;

public record CauseIn(long byId, Long planId, Integer deedIndex) {

    public static CauseIn by(long id) {
        return new CauseIn(id, null, null);
    }
}
