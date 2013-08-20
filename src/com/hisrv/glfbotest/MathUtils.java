package com.hisrv.glfbotest;

public class MathUtils {
	public static int nextPowerOfTwo(int x) {
		int r = 1;
		while (r < x) {
			r = r * 2;
		}
		return r;
	}
}
