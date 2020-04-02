#!/usr/bin/env python3
#piecemiel subtraction
for a,b in [[0xE, 0xF], [0xF, 0xE]]:
    if a > b:
        out = (a-b)&0xF
        carry = 0
    elif a < b:
        out = (a-b)&0xF
        carry = (b-a)&0xF
    else:
        out = 0
        carry = 0
    print(hex(out), hex(carry))
