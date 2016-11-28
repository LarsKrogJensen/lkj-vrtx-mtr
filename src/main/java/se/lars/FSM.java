package se.lars;


import io.vertx.core.eventbus.Message;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


public class FSM
{

    public static <TState> State<TState> when(TState state)
    {
        return new State<>(state);
    }



    public static class State<TState>
    {
        private TState _state;
        private List<Match<TState, ? super Object>> _matchers = new ArrayList<>();

        public State(TState state)
        {
            _state = state;
        }

        public TState state()
        {
            return _state;
        }

        public <TValue> State<TState> match(Class<TValue> type,
                                            Function<Message<TValue>, TState> handler)
        {
            return match(type, tValue -> true, handler);
        }

        public <TValue> State<TState> match(Class<TValue> type,
                                            Predicate<TValue> predicate,
                                            Function<Message<TValue>, TState> handler)
        {
            _matchers.add(new Match(type, predicate, handler));
            return this;
        }

        public Stream<Match<TState, Object>> matchers()
        {
            return _matchers.stream();
        }
    }

    static class Match<TState, TValue>
    {
        private Class<TValue> _type;
        private Function<Message<TValue>, TState> _handler;
        private Predicate<TValue> _predicate;


        public Match(Class<TValue> type,
                     Predicate<TValue> predicate,
                     Function<Message<TValue>, TState> handler)
        {
            _type = type;
            _predicate = predicate;
            _handler = handler;
        }

        public Class<TValue> type()
        {
            return _type;
        }

        public Predicate<TValue> predicate()
        {
            return _predicate;
        }

        public Function<Message<TValue>, TState> handler()
        {
            return _handler;
        }
    }
}
