package se.lars;

/**
 * Created by Lena on 2016-11-12.
 */
public class SomeBean  implements KryoObject
{
    private String _value;

    public SomeBean()
    {
    }

    public SomeBean(String value)
    {
        _value = value;
    }

    public String getValue()
    {
        return _value;
    }

    @Override
    public String toString()
    {
        return "SomeBean{" +
            "_value='" + _value + '\'' +
            '}';
    }

    @Override
    public KryoObject copy()
    {
        return new SomeBean(getValue());
    }
}
