package se.lars;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestVerticle
    extends AbstractVerticle
{
    private static Logger _log = LoggerFactory.getLogger(RestVerticle.class);

    private HttpServer _httpServer;

    @Override
    public void start(Future<Void> startFuture)
        throws Exception
    {

        Router apiv1 = Router.router(vertx);
        apiv1.route().handler(new ApiV1Handler(vertx.eventBus()));

        Router apiv2 = Router.router(vertx);
        apiv2.route().handler(new ApiV2Handler());


        Router router = Router.router(vertx);
        router.mountSubRouter("/api/v1", apiv1);
        router.mountSubRouter("/api/v2", apiv2);

        vertx.eventBus().<SomeBean>consumer("some.address", message -> {
            System.out.println("Received message: " + message.body());
        });

        _httpServer = vertx.createHttpServer()
                           .requestHandler(router::accept)
                           .listen(config().getInteger("http.port"), result -> {
                               if (result.succeeded()) {
                                   _log.info("HttpServer successfully started at {}",result.result().actualPort());
                                   startFuture.succeeded();
                               } else {
                                   _log.error("HttpServer failed to launch.");
                                   startFuture.fail(result.cause());
                               }
                           });
    }
}
