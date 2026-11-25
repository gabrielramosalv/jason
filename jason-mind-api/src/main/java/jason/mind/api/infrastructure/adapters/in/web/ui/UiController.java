package jason.mind.api.infrastructure.adapters.in.web.ui;

import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;

/**
 * Controller that serves the entry page of the Mind Inspector UI at the root of the server (D-154).
 * The other files of the UI (scripts, styles) are served by the static content of Peel Web.
 *
 * @author Gabriel Lima
 */
@Controller("/")
public class UiController {

    /** Classpath folder where the build of {@code jason-mind-ui} puts the UI. */
    public static final String RESOURCE_FOLDER = "mind-ui";

    /**
     * Creates the controller of the UI entry page.
     */
    public UiController() {
    }

    /**
     * Returns the entry page of the UI.
     *
     * @return the page, or not found when this build of the jar has no UI
     */
    @Mapping
    public Response page() {
        if (Thread.currentThread().getContextClassLoader().getResource(RESOURCE_FOLDER + "/index.html") == null) {
            return Responses.notFound("this build has no UI; build jason-mind-ui or use the API under /api");
        }
        return Responses.page("index.html");
    }
}
