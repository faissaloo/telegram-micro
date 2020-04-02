#!/usr/bin/env python3
void mult64to128(long op1, long op2, long *hi, long *lo)
{
    long u1 = (op1 & 0xffffffff);
    long v1 = (op2 & 0xffffffff);
    long t = (u1 * v1);
    long w3 = (t & 0xffffffff);
    long k = (t >> 32);

    op1 >>= 32;
    t = (op1 * v1) + k;
    k = (t & 0xffffffff);
    long w1 = (t >> 32);

    op2 >>= 32;
    t = (u1 * op2) + k;
    k = (t >> 32);

    *hi = (op1 * op2) + w1 + k;
    *lo = (t << 32) + w3;
}
