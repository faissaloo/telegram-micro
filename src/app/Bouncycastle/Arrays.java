package bouncycastle;

/**
 * General array utilities.
 */
public class Arrays
{
    public static void fill(
        int[] array,
        int value)
    {
        for (int i = 0; i < array.length; i++)
        {
            array[i] = value;
        }
    }

    public static byte[] clone(byte[] data)
    {
        if (data == null)
        {
            return null;
        }
        byte[] copy = new byte[data.length];

        System.arraycopy(data, 0, copy, 0, data.length);

        return copy;
    }

    public static byte[][] clone(byte[][] data)
    {
        if (data == null)
        {
            return null;
        }

        byte[][] copy = new byte[data.length][];

        for (int i = 0; i != copy.length; i++)
        {
            copy[i] = clone(data[i]);
        }

        return copy;
    }

    public static int[] clone(int[] data)
    {
        if (data == null)
        {
            return null;
        }
        int[] copy = new int[data.length];

        System.arraycopy(data, 0, copy, 0, data.length);

        return copy;
    }

    public static long[] clone(long[] data)
    {
        if (data == null)
        {
            return null;
        }
        long[] copy = new long[data.length];

        System.arraycopy(data, 0, copy, 0, data.length);

        return copy;
    }
}
