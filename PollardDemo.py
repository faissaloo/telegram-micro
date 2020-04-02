#!/usr/bin/env python3
#https://en.wikipedia.org/wiki/Pollard%27s_rho_algorithm
import random

#Euclidian
def euclidian_greatest_common_denominator(a, b):
	a = abs(a)
	b = abs(b)
	while a:
		a, b = b % a, a
	return b

#Progressively increases the value in smaller and smaller increments
def limited_polynomial(current_value, limit):
	val = ((current_value ** 2) % limit + 1) % limit
	#the increment just adjusts the phase(?) of the limited polynomial
	return val

def brent(to_factorise):
	iteration = 0;
	if to_factorise % 2 == 0:
		return 2
	fast_pointer = 1
	nontrivial_denominator, search_range, initial_guess = 1, 1, 1
	while nontrivial_denominator == 1:
		print("Iteration:", iteration)
		iteration += 1
		slow_pointer = fast_pointer
		for i in range(search_range):
			fast_pointer = limited_polynomial(fast_pointer, to_factorise)
		already_searched = 0
		while already_searched < search_range and nontrivial_denominator == 1:
			for i in range(min(1, search_range - already_searched)):
				fast_pointer = limited_polynomial(fast_pointer, to_factorise)
				initial_guess = initial_guess * (abs(slow_pointer - fast_pointer)) % to_factorise
			nontrivial_denominator = euclidian_greatest_common_denominator(initial_guess, to_factorise)
			already_searched += 1
		search_range *= 2

	if nontrivial_denominator == to_factorise:
		while True:
			fast_pointer = limited_polynomial(fast_pointer, to_factorise)
			nontrivial_denominator = euclidian_greatest_common_denominator(abs(slow_pointer - fast_pointer), to_factorise)
			if nontrivial_denominator > 1:
				break
	return nontrivial_denominator

n=2385975044415226357
p=brent(n)
q=n//p
p,q=sorted([p,q])
print("n<{}> = p<{}> * q<{}>".format(n,p,q))
print("n=", n)
print("p=", p)
print("q=", q)
print("p < q", p < q)
print("p > q", p > q)
