package se.lars;

import com.google.common.reflect.ClassPath;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import javaslang.API;
import javaslang.collection.List;
import org.joox.JOOX;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static javaslang.API.*;
import static javaslang.Predicates.instanceOf;

public abstract class VerticleBase
    extends AbstractVerticle
{
    protected <T> void receive(String address, Stream<EventConsumer<?>> consumers)
    {
        vertx.eventBus().consumer(address, event -> {
            consumers.filter(consumer -> consumer.type().equals(event.body().getClass()))

                     .findFirst()
                     .ifPresent(consumer -> {
                         //consumer.handler().accept((Message<?>)event.body());
                     });

        });
    }

    protected <T> EventConsumer<T> match(Class<T> type, Consumer<Message<T>> consumer)
    {
        return new EventConsumer<T>(type, consumer);
    }

    public class EventConsumer<T>
    {
        Class<T> _type;
        Consumer<Message<T>> _consumer;

        public EventConsumer(Class<T> type, Consumer<Message<T>> handler)
        {
            _type = type;
            _consumer = handler;
        }

        public Class<T> type()
        {
            return _type;
        }

        public Consumer<Message<T>> handler()
        {
            return _consumer;
        }
    }
}

class TestVerticleBase
    extends VerticleBase
{
    public TestVerticleBase()
    {
    }

    @Override
    public void start()
        throws Exception
    {
        receive("1234", Stream.of(
            match(Integer.class, message -> {

            }),
            match(String.class, message -> {

            })

        ));

        vertx.eventBus().consumer("1234", event -> {
            Match(event.body()).of(
                Case(instanceOf(Integer.class), () -> {
                    return Void.TYPE;
                }),
                Case($("2"), "two"),
                Case($(), "?")
            );
        });
        Object i = 1;

    }

    public static void main(String[] args)
        throws IOException, SAXException
    {
        Integer fold = List.of(1, 2, 3, 4)
                           .fold(0, (integer, integer2) -> {
                               System.out.println(integer + " + " + integer2);
                               return integer + integer2;
                           });
        System.out.println("fold: " + fold);

        Integer foldLeft = List.of(1, 2, 3, 4)
                               .foldLeft(0, (integer, integer2) -> {
                                   System.out.println(integer + " + " + integer2);
                                   return integer + integer2;
                               });
        System.out.println("fold left: " + foldLeft);

        Integer foldRight = List.of(1, 2, 3, 4)
                                .foldRight(0, (integer, integer2) -> {
                                    System.out.println(integer + " + " + integer2);
                                    return integer + integer2;
                                });
        System.out.println("fold right: " + foldRight);


        List<Integer> s1 = List.of(1, 2, 3, 4);
        List<String> s2 = List.of("1", "2");

        s1.zipAll(s2, null, null).forEach(p -> {
            System.out.println(p);
        });


        String fold1 = List.of("Lars", "is", "awesome")
                           .intersperse(", ")
                           .fold("", String::concat);
        System.out.println("Folded words: " + fold1);

        //String fold2 = Stream.of("Lars", "is", "awesome")
        //                     .intersperse(", ")
        //                     .fold("", String::concat);
        //System.out.println("Folded2 words: " + fold2);

        for (int i = 0; i < 10; i++) {
            System.out.println((Integer)i);
        }

        HashMap<String, String> map =
            new HashMap<String, String>()
            {{
                put("1", "a");
                put("2", "b");
            }};

        ArrayList<String> list = new ArrayList<String>()
        {{
            add("sasasa");
            add("34343");
        }};

        System.out.println(map);

        JOOX.$(ClassLoader.getSystemResourceAsStream("doc.xml"))
            .find("book#1")
            .attrs("id")
            .forEach(System.out::println);
    }
}
