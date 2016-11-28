package se.lars;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ApiV2Handler
    implements Handler<RoutingContext>
{
    @Override
    public void handle(RoutingContext routingContext)
    {
        String resource = routingContext.request().path().replace(routingContext.mountPoint(), "");
        routingContext.response().end("ApiV2 OK: " + resource);
    }
}
