package dht.rush.utils;

public class RushUtil {
    public static final long MAX_VALUE = 0xFFFFFFFFL;
    public static final double MAX_NODE = 15359.0;

    public static Double rushHash(String s, int r, String id) {
        long a = s.hashCode();
        long b = r & MAX_VALUE;
        long c = id.hashCode();

        a = subtract(a, b);
        a = subtract(a, c);
        a = xor(a, c >> 13);

        b = subtract(b, c);
        b = subtract(b, a);
        b = xor(b, leftShift(a, 8));

        c = subtract(c, a);
        c = subtract(c, b);
        c = xor(c, (b >> 13));

        a = subtract(a, b);
        a = subtract(a, c);
        a = xor(a, (c >> 12));

        b = subtract(b, c);
        b = subtract(b, a);
        b = xor(b, leftShift(a, 16));

        c = subtract(c, a);
        c = subtract(c, b);
        c = xor(c, (b >> 5));

        a = subtract(a, b);
        a = subtract(a, c);
        a = xor(a, (c >> 3));

        b = subtract(b, c);
        b = subtract(b, a);
        b = xor(b, leftShift(a, 10));

        c = subtract(c, a);
        c = subtract(c, b);
        c = xor(c, (b >> 15));

        return (c % MAX_NODE) / MAX_NODE;
    }

    private static long subtract(long a, long b) {
        return (a - b) & MAX_VALUE;
    }

    public static long leftShift(long a, int shift) {
        return (a << shift) & MAX_VALUE;
    }

    private static long xor(long a, long xor) {
        return (a ^ xor) & MAX_VALUE;
    }
}