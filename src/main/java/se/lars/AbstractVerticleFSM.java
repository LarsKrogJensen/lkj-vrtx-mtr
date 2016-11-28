package se.lars;


import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static se.lars.FSM.when;

//import static se.lars.FSM.when;

public class AbstractVerticleFSM<TState>
    extends AbstractVerticle
{
    private List<MessageConsumer<?>> _consumers = new ArrayList<>();
    private TState _currentState;

    protected void init(TState state)
    {
        _currentState = state;
    }

    void receive(String address, Stream<FSM.State<TState>> stateBuilders)
    {
        // Capture states to be able to stream over the states for each received message
        List<FSM.State<TState>> states = stateBuilders.collect(Collectors.toList());

        MessageConsumer<Object> consumer = vertx.eventBus().consumer(address, event -> {
            states.stream()
                  .filter(state -> state.state() == _currentState)
                  .flatMap(FSM.State::matchers)
                  .filter(match -> match.type().equals(event.body().getClass()))
                  .filter(match -> match.predicate().test(event.body()))
                  .findFirst()
                  .ifPresent(match -> {
                      _currentState = match.handler().apply(event);
                  });
        });
        _consumers.add(consumer);
    }

    static class MyStatefulVerticle
        extends AbstractVerticleFSM<MyStatefulVerticle.State>
    {
        enum State
        {
            Init, Started
        };

        @Override
        public void start()
            throws Exception
        {
            init(State.Init);

            receive("1234", Stream.of(
                when(State.Init)
                    .match(Integer.class, (message) -> {
                        System.out.println("INIT: recevied: " + message.body());
                        return State.Init;
                    })
                    .match(String.class, (message) -> {
                        System.out.println("INIT: recevied: " + message.body());
                        return State.Started;
                    }),
                when(State.Started)
                    .match(Integer.class, (message) -> {
                        System.out.println("STARTED: recevied: " + message.body());
                        return State.Started;
                    })
                    .match(String.class, (message) -> {
                        System.out.println("STARTED: recevied: " + message.body());
                        return State.Init;
                    })));
        }

        public static void main(String[] args)
        {
            Vertx vertx = Vertx.vertx();
            vertx.deployVerticle(new MyStatefulVerticle(), event -> {
                vertx.eventBus().send("1234", 1);
                vertx.eventBus().send("1234", "forward");
                vertx.eventBus().send("1234", 2);
                vertx.eventBus().send("1234", "forward");
            });
        }
    }
}
