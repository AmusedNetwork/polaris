package uk.co.ractf.polaris.api.andromeda;

@Deprecated
public class AndromedaChallengeSubmitResponse {

    private final String id;

    public AndromedaChallengeSubmitResponse(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
