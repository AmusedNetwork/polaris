package uk.co.ractf.polaris.api.challenge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Contract;
import uk.co.ractf.polaris.api.common.JsonRepresentable;
import uk.co.ractf.polaris.api.pod.Pod;

import java.util.List;
import java.util.Objects;

/**
 * Represents a challenge that can run on Polaris, comprised of one or more {@link Pod}s
 *
 * <pre>
 * {
 *   "id": "example",
 *   "pods": []
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Challenge extends JsonRepresentable {

    private final String id;
    private final List<Pod> pods;

    /**
     * Create a challenge
     *
     * @param id   the id of the challenge
     * @param pods the pods making up the challenge
     */
    @Contract(pure = true)
    public Challenge(@JsonProperty("id") final String id, @JsonProperty("pods") final List<Pod> pods) {
        this.id = id;
        this.pods = pods;
    }

    public String getID() {
        return id;
    }

    public List<Pod> getPods() {
        return pods;
    }

    /**
     * Gets a {@link Pod} from this challenge that has a given id
     *
     * @param id the id of the pod
     * @return the pod
     */
    @JsonIgnoreProperties
    public Pod getPod(final String id) {
        for (final Pod pod : pods) {
            if (pod.getID().equals(id)) {
                return pod;
            }
        }

        return null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Challenge challenge = (Challenge) o;
        return Objects.equals(id, challenge.id) && Objects.equals(pods, challenge.pods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pods);
    }
}
