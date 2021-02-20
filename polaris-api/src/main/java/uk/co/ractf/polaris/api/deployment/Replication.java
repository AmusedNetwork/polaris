package uk.co.ractf.polaris.api.deployment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.Contract;

/**
 * Represents the strategy that a {@link Deployment} should be replicated with.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StaticReplication.class, name = "static")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Replication {

    private final String type;

    /**
     * Abstract constructor for Replication
     *
     * @param type the type of replication
     */
    @Contract(pure = true)
    protected Replication(@JsonProperty("type") final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

}