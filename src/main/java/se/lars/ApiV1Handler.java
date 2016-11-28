package se.lars;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.RoutingContext;

public class ApiV1Handler
    implements Handler<RoutingContext>
{
    private final EventBus _eventBus;

    public ApiV1Handler(EventBus eventBus)
    {
        _eventBus = eventBus;

    }

    @Override
    public void handle(RoutingContext routingContext)
    {
        _eventBus.publish("some.address", new SomeBean("lars"));

        routingContext.response().end("ApiV1 OK: " +routingContext.request().uri());
    }
}
