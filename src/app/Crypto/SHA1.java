package crypto;

//Written by Mariotaku for Twidere J2ME
//SHA1 is no longer safe, only use it where the protocol requires it
public class SHA1 {
	public static final int HASH_SIZE = 20;

	private final int[] abcde = { 0x67452301, 0xefcdab89, 0x98badcfe,
			0x10325476, 0xc3d2e1f0 };

	// Summary data storage array
	private int[] digestInt = new int[5];

	// The calculation process for temporary data storage array
	private int[] tmpData = new int[80];

	// Calculates sha -1 Summary
	private int process_input_bytes(byte[] bytedata) {
		// Keen understanding of constant
		System.arraycopy(abcde, 0, digestInt, 0, abcde.length);

		// Formatted input byte array, Supplement 10 and length data
		byte[] newbyte = byteArrayFormatData(bytedata);

		// Gets the data Digest calculation data cell number
		int MCount = newbyte.length / 64;

		// Cycling on each data cell for Digest calculation
		for (int pos = 0; pos < MCount; pos++) {
			// Each unit of data is converted to a 16 integer data, and save it
			// to the tmpData of the first 16 array element
			for (int j = 0; j < 16; j++) {
				tmpData[j] = byteArrayToInt(newbyte, (pos * 64) + (j * 4));
			}

			// Summary of the evaluation function
			encrypt();
		}

		return 20;
	}

	// Formatted input byte array format
	private byte[] byteArrayFormatData(byte[] bytedata) {
		// The number of supplementary 0
		int zeros = 0;

		// The total number of digits after the invigorating bit
		int size = 0;

		// The original data length
		int n = bytedata.length;

		// Die 64 bits of the remaining after
		int m = n % 64;

		// The calculation of the number of 0, and to add the length 10 total
		if (m < 56) {
			zeros = 55 - m;
			size = n - m + 64;
		} else if (m == 56) {
			zeros = 63;
			size = n + 8 + 64;
		} else {
			zeros = 63 - m + 56;
			size = (n + 64) - m + 64;
		}

		// BU-generated after the contents of the new array
		byte[] newbyte = new byte[size];
		// Copy the front of the array
		System.arraycopy(bytedata, 0, newbyte, 0, n);

		// Get array Append data element
		int l = n;
		// Supplement 1 operation
		newbyte[l++] = (byte) 0x80;

		// Complement 0 operations
		for (int i = 0; i < zeros; i++) {
			newbyte[l++] = (byte) 0x00;
		}

		// Calculated data length, supplement data length bit 8 bytes, long
		// integer
		long N = (long) n * 8;
		byte h8 = (byte) (N & 0xFF);
		byte h7 = (byte) ((N >> 8) & 0xFF);
		byte h6 = (byte) ((N >> 16) & 0xFF);
		byte h5 = (byte) ((N >> 24) & 0xFF);
		byte h4 = (byte) ((N >> 32) & 0xFF);
		byte h3 = (byte) ((N >> 40) & 0xFF);
		byte h2 = (byte) ((N >> 48) & 0xFF);
		byte h1 = (byte) (N >> 56);
		newbyte[l++] = h1;
		newbyte[l++] = h2;
		newbyte[l++] = h3;
		newbyte[l++] = h4;
		newbyte[l++] = h5;
		newbyte[l++] = h6;
		newbyte[l++] = h7;
		newbyte[l++] = h8;

		return newbyte;
	}

	private int f1(int x, int y, int z) {
		return (x & y) | (~x & z);
	}

	private int f2(int x, int y, int z) {
		return x ^ y ^ z;
	}

	private int f3(int x, int y, int z) {
		return (x & y) | (x & z) | (y & z);
	}

	private int f4(int x, int y) {
		return (x << y) | x >>> (32 - y);
	}

	// Unit summary calculation function
	private void encrypt() {
		for (int i = 16; i <= 79; i++) {
			tmpData[i] = f4(tmpData[i - 3] ^ tmpData[i - 8] ^ tmpData[i - 14]
			                                                          ^ tmpData[i - 16], 1);
		}

		int[] tmpabcde = new int[5];

		for (int i1 = 0; i1 < tmpabcde.length; i1++) {
			tmpabcde[i1] = digestInt[i1];
		}

		for (int j = 0; j <= 19; j++) {
			int tmp = f4(tmpabcde[0], 5)
			+ f1(tmpabcde[1], tmpabcde[2], tmpabcde[3]) + tmpabcde[4]
			                                                       + tmpData[j] + 0x5a827999;
			tmpabcde[4] = tmpabcde[3];
			tmpabcde[3] = tmpabcde[2];
			tmpabcde[2] = f4(tmpabcde[1], 30);
			tmpabcde[1] = tmpabcde[0];
			tmpabcde[0] = tmp;
		}

		for (int k = 20; k <= 39; k++) {
			int tmp = f4(tmpabcde[0], 5)
			+ f2(tmpabcde[1], tmpabcde[2], tmpabcde[3]) + tmpabcde[4]
			                                                       + tmpData[k] + 0x6ed9eba1;
			tmpabcde[4] = tmpabcde[3];
			tmpabcde[3] = tmpabcde[2];
			tmpabcde[2] = f4(tmpabcde[1], 30);
			tmpabcde[1] = tmpabcde[0];
			tmpabcde[0] = tmp;
		}

		for (int l = 40; l <= 59; l++) {
			int tmp = f4(tmpabcde[0], 5)
			+ f3(tmpabcde[1], tmpabcde[2], tmpabcde[3]) + tmpabcde[4]
			                                                       + tmpData[l] + 0x8f1bbcdc;
			tmpabcde[4] = tmpabcde[3];
			tmpabcde[3] = tmpabcde[2];
			tmpabcde[2] = f4(tmpabcde[1], 30);
			tmpabcde[1] = tmpabcde[0];
			tmpabcde[0] = tmp;
		}

		for (int m = 60; m <= 79; m++) {
			int tmp = f4(tmpabcde[0], 5)
			+ f2(tmpabcde[1], tmpabcde[2], tmpabcde[3]) + tmpabcde[4]
			                                                       + tmpData[m] + 0xca62c1d6;
			tmpabcde[4] = tmpabcde[3];
			tmpabcde[3] = tmpabcde[2];
			tmpabcde[2] = f4(tmpabcde[1], 30);
			tmpabcde[1] = tmpabcde[0];
			tmpabcde[0] = tmp;
		}

		for (int i2 = 0; i2 < tmpabcde.length; i2++) {
			digestInt[i2] = digestInt[i2] + tmpabcde[i2];
		}

		for (int n = 0; n < tmpData.length; n++) {
			tmpData[n] = 0;
		}
	}

	// 4 A byte array to convert to an integer
	private int byteArrayToInt(byte[] bytedata, int i) {
		return ((bytedata[i] & 0xff) << 24) | ((bytedata[i + 1] & 0xff) << 16)
		| ((bytedata[i + 2] & 0xff) << 8) | (bytedata[i + 3] & 0xff);
	}

	// Integer is converted to a 4-byte array
	private void intToByteArray(int intValue, byte[] byteData, int i) {
		byteData[i] = (byte) (intValue >>> 24);
		byteData[i + 1] = (byte) (intValue >>> 16);
		byteData[i + 2] = (byte) (intValue >>> 8);
		byteData[i + 3] = (byte) intValue;
	}

	// To convert a hexadecimal string bytes
	private static String byteToHexString(byte ib) {
		char[] Digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
				'b', 'c', 'd', 'e', 'f' };
		char[] ob = new char[2];
		ob[0] = Digit[(ib >>> 4) & 0X0F];
		ob[1] = Digit[ib & 0X0F];

		String s = new String(ob);

		return s;
	}

	// Converts an array of bytes into a string of hexadecimal characters
	private static String byteArrayToHexString(byte[] bytearray) {
		String strDigest = "";

		for (int i = 0; i < bytearray.length; i++) {
			strDigest += byteToHexString(bytearray[i]);
		}

		return strDigest;
	}

	// Calculates sha -1 Summary returns the corresponding byte array
	public byte[] digest(byte[] byteData) {
		process_input_bytes(byteData);

		byte[] digest = new byte[20];

		for (int i = 0; i < digestInt.length; i++) {
			intToByteArray(digestInt[i], digest, i * 4);
		}

		return digest;
	}
}
