package uk.co.ractf.polaris.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import uk.co.ractf.polaris.api.host.HostInfo;
import uk.co.ractf.polaris.controller.Controller;
import uk.co.ractf.polaris.host.Host;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Resource providing API endpoints for {@link HostInfo} and {@link Host} objects.
 * <p>
 * Roles defined: HOST_GET
 */
@Path("/hosts")
@Produces(MediaType.APPLICATION_JSON)
public class HostResource {

    private final Controller controller;

    @Inject
    public HostResource(final Controller controller) {
        this.controller = controller;
    }

    /**
     * Return a {@link Map} of host to {@link HostInfo} for all hosts matching the host id regex. Results are only
     * filtered if a filter is not omitted.
     *
     * @param filter The regex to filter host ids by
     * @return the matching hosts
     */
    @GET
    @Timed
    @ExceptionMetered
    @RolesAllowed("HOST_GET")
    @Operation(summary = "Get Hosts", tags = {"Host"},
            description = "Gets a list of hosts currently registered and info about them")
    public Map<String, HostInfo> listHostInfo(@QueryParam("filter") @DefaultValue("") final String filter) {
        final Map<String, HostInfo> hostInfoList = new HashMap<>();
        final Pattern pattern = Pattern.compile(filter);

        for (final Host host : controller.getHosts().values()) {
            if (pattern.matcher(filter).find()) {
                hostInfoList.put(host.getId(), host.getHostInfo());
            }
        }
        return hostInfoList;
    }

    /**
     * Gets {@link HostInfo} from the host's id.
     *
     * @param id host id
     * @return host info
     */
    @GET
    @Timed
    @Path("/{id}")
    @ExceptionMetered
    @RolesAllowed("HOST_GET")
    @Operation(summary = "Get Host", tags = {"Host"},
            description = "Gets a host by id")
    public HostInfo getHostInfo(@PathParam("id") final String id) {
        final Host host = controller.getHost(id);
        if (host == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return host.getHostInfo();
    }

}
