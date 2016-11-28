
package se.lars;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

import java.io.ByteArrayOutputStream;
import java.util.function.Supplier;

public class KryoCodec<T>
    implements MessageCodec<T, T>
{
    private static final ThreadLocal<Kryo> kryos = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.register(SomeBean.class, 0);
        return kryo;
    });

    private static final ThreadLocal<Output> out = ThreadLocal.withInitial(() -> new UnsafeOutput(new ByteArrayOutputStream()));

    private Class<T> _type;

    public KryoCodec(Class<T> type)
    {
        _type = type;
    }

    @Override
    public void encodeToWire(Buffer buffer, T obj)
    {
        Output output = out.get();
        output.setPosition(0); // reset output
        kryos.get().writeObject(output, obj);
        byte[] bytes = output.toBytes();
        buffer.appendInt(bytes.length);
        buffer.appendBytes(bytes);
    }

    @Override
    public T decodeFromWire(int pos, Buffer buffer)
    {
        int length = buffer.getInt(pos);
        pos += 4;
        byte[] encoded = buffer.getBytes(pos, pos + length);

        try (Input input = new UnsafeInput(encoded)) {
            return (T)kryos.get().<T>readObject(input, _type);
        }
    }

    @Override
    public T transform(T o)
    {
        return o;
    }

    @Override
    public String name()
    {
        return "kryo";
    }

    @Override
    public byte systemCodecID()
    {
        return -1;
    }
}
