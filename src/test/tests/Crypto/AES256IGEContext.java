package test;

import crypto.AES256IGE;

public class AES256IGEContext {
  public static class SubBytesTest extends FocusedTest {
    public String label() {
      return "It can perform the SubBytes step";
    }
    public void test() throws TestFailureException {
      AES256IGE subject = new AES256IGE();
      subject.state = new byte[] {
        (byte)0x10, (byte)0x34, (byte)0x69, (byte)0x4A,
        (byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC,
        (byte)0x10, (byte)0x34, (byte)0x69, (byte)0x4A,
        (byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC,
      };
      subject.substitute_bytes();
      expect(subject.state, new byte[] {
        (byte)0xCA, (byte)0x18, (byte)0xF9, (byte)0xD6,
        (byte)0x16, (byte)0x28, (byte)0xC1, (byte)0x4B,
        (byte)0xCA, (byte)0x18, (byte)0xF9, (byte)0xD6,
        (byte)0x16, (byte)0x28, (byte)0xC1, (byte)0x4B
      });
    }
  }

  public static class MixColumnsTest extends FocusedTest {
    public String label() {
      return "It can perform the MixColumns step";
    }
    public void test() throws TestFailureException {
      AES256IGE subject = new AES256IGE();
      subject.state = new byte[] {
        (byte)0x10, (byte)0x34, (byte)0x69, (byte)0x4A,
        (byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC,
        (byte)0x10, (byte)0x34, (byte)0x69, (byte)0x4A,
        (byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC,
      };
      subject.mix_columns();
      expect(subject.state, new byte[] {
        (byte)0x5F, (byte)0x89, (byte)0x28, (byte)0xF9,
        (byte)0xDD, (byte)0x88, (byte)0xFF, (byte)0xAA,
        (byte)0x5F, (byte)0x89, (byte)0x28, (byte)0xF9,
        (byte)0xDD, (byte)0x88, (byte)0xFF, (byte)0xAA
      });
    }
  }

  public static class ShiftRowsTest extends FocusedTest {
    public String label() {
      return "It can perform the ShiftRows step";
    }
    public void test() throws TestFailureException {
      AES256IGE subject = new AES256IGE();
      subject.state = new byte[] {
        (byte)0x10, (byte)0x34, (byte)0x69, (byte)0x4A,
        (byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC,
        (byte)0x10, (byte)0x34, (byte)0x69, (byte)0x4A,
        (byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC,
      };
      subject.shift_rows();
      expect(subject.state, new byte[] {
        (byte)0x10, (byte)0xEE, (byte)0x69, (byte)0xCC,
        (byte)0xFF, (byte)0x34, (byte)0xDD, (byte)0x4A,
        (byte)0x10, (byte)0xEE, (byte)0x69, (byte)0xCC,
        (byte)0xFF, (byte)0x34, (byte)0xDD, (byte)0x4A,
      });
    }
  }

  public static class RotateLeftTest extends FocusedTest {
    public String label() {
      return "It can rotate a set of bytes";
    }
    public void test() throws TestFailureException {
      AES256IGE subject = new AES256IGE();
      byte[] result = subject.rotate_left(new byte[] { (byte)0x1, (byte)0x2, (byte)0x3, (byte)0x4 });
      expect(result, new byte[] { (byte)0x2, (byte)0x3, (byte)0x4, (byte)0x1 });
    }
  }

  public static class GaloisField2PowerTest extends FocusedTest {
    public String label() {
      return "It can exponentiate 2 within the galois field (aka 'rcon')";
    }
    public void test() throws TestFailureException {
      AES256IGE subject = new AES256IGE();
      expect(subject.galois_field_2_power((byte)0x69), (byte)4);
    }
  }

  //Add a test for the round key generation
  public static class AddRoundKeyTest extends FocusedTest {
    public String label() {
      return "It can apply the round key";
    }
    public void test() throws TestFailureException {
      AES256IGE subject = new AES256IGE();
      subject.key_schedule = new byte[] {
        (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0x62, (byte)0x63, (byte)0x63, (byte)0x63,
        (byte)0x62, (byte)0x63, (byte)0x63, (byte)0x63,
        (byte)0x62, (byte)0x63, (byte)0x63, (byte)0x63,
        (byte)0x62, (byte)0x63, (byte)0x63, (byte)0x63,
        (byte)0xaa, (byte)0xfb, (byte)0xfb, (byte)0xfb,
        (byte)0xaa, (byte)0xfb, (byte)0xfb, (byte)0xfb,
        (byte)0xaa, (byte)0xfb, (byte)0xfb, (byte)0xfb,
        (byte)0xaa, (byte)0xfb, (byte)0xfb, (byte)0xfb,
        (byte)0x6f, (byte)0x6c, (byte)0x6c, (byte)0xcf,
        (byte)0x0d, (byte)0x0f, (byte)0x0f, (byte)0xac,
        (byte)0x6f, (byte)0x6c, (byte)0x6c, (byte)0xcf,
        (byte)0x0d, (byte)0x0f, (byte)0x0f, (byte)0xac,
        (byte)0x7d, (byte)0x8d, (byte)0x8d, (byte)0x6a,
        (byte)0xd7, (byte)0x76, (byte)0x76, (byte)0x91,
        (byte)0x7d, (byte)0x8d, (byte)0x8d, (byte)0x6a,
        (byte)0xd7, (byte)0x76, (byte)0x76, (byte)0x91,
        (byte)0x53, (byte)0x54, (byte)0xed, (byte)0xc1,
        (byte)0x5e, (byte)0x5b, (byte)0xe2, (byte)0x6d,
        (byte)0x31, (byte)0x37, (byte)0x8e, (byte)0xa2,
        (byte)0x3c, (byte)0x38, (byte)0x81, (byte)0x0e,
        (byte)0x96, (byte)0x8a, (byte)0x81, (byte)0xc1,
        (byte)0x41, (byte)0xfc, (byte)0xf7, (byte)0x50,
        (byte)0x3c, (byte)0x71, (byte)0x7a, (byte)0x3a,
        (byte)0xeb, (byte)0x07, (byte)0x0c, (byte)0xab,
        (byte)0x9e, (byte)0xaa, (byte)0x8f, (byte)0x28,
        (byte)0xc0, (byte)0xf1, (byte)0x6d, (byte)0x45,
        (byte)0xf1, (byte)0xc6, (byte)0xe3, (byte)0xe7,
        (byte)0xcd, (byte)0xfe, (byte)0x62, (byte)0xe9,
        (byte)0x2b, (byte)0x31, (byte)0x2b, (byte)0xdf,
        (byte)0x6a, (byte)0xcd, (byte)0xdc, (byte)0x8f,
        (byte)0x56, (byte)0xbc, (byte)0xa6, (byte)0xb5,
        (byte)0xbd, (byte)0xbb, (byte)0xaa, (byte)0x1e,
        (byte)0x64, (byte)0x06, (byte)0xfd, (byte)0x52,
        (byte)0xa4, (byte)0xf7, (byte)0x90, (byte)0x17,
        (byte)0x55, (byte)0x31, (byte)0x73, (byte)0xf0,
        (byte)0x98, (byte)0xcf, (byte)0x11, (byte)0x19,
        (byte)0x6d, (byte)0xbb, (byte)0xa9, (byte)0x0b,
        (byte)0x07, (byte)0x76, (byte)0x75, (byte)0x84,
        (byte)0x51, (byte)0xca, (byte)0xd3, (byte)0x31,
        (byte)0xec, (byte)0x71, (byte)0x79, (byte)0x2f,
        (byte)0xe7, (byte)0xb0, (byte)0xe8, (byte)0x9c,
        (byte)0x43, (byte)0x47, (byte)0x78, (byte)0x8b,
        (byte)0x16, (byte)0x76, (byte)0x0b, (byte)0x7b,
        (byte)0x8e, (byte)0xb9, (byte)0x1a, (byte)0x62,
        (byte)0x74, (byte)0xed, (byte)0x0b, (byte)0xa1,
        (byte)0x73, (byte)0x9b, (byte)0x7e, (byte)0x25,
        (byte)0x22, (byte)0x51, (byte)0xad, (byte)0x14,
        (byte)0xce, (byte)0x20, (byte)0xd4, (byte)0x3b,
        (byte)0x10, (byte)0xf8, (byte)0x0a, (byte)0x17,
        (byte)0x53, (byte)0xbf, (byte)0x72, (byte)0x9c,
        (byte)0x45, (byte)0xc9, (byte)0x79, (byte)0xe7,
        (byte)0xcb, (byte)0x70, (byte)0x63, (byte)0x85
      };
      subject.state = new byte[] {
        (byte)0x10, (byte)0xEE, (byte)0x69, (byte)0xCC,
        (byte)0xFF, (byte)0x34, (byte)0xDD, (byte)0x4A,
        (byte)0x10, (byte)0xEE, (byte)0x69, (byte)0xCC,
        (byte)0xFF, (byte)0x34, (byte)0xDD, (byte)0x4A,
      };
      subject.current_round = 0;
      subject.add_round_key();
      expect(subject.state, new byte[] {
        (byte)0x10, (byte)0xEE, (byte)0x69, (byte)0xCC,
        (byte)0xFF, (byte)0x34, (byte)0xDD, (byte)0x4A,
        (byte)0x10, (byte)0xEE, (byte)0x69, (byte)0xCC,
        (byte)0xFF, (byte)0x34, (byte)0xDD, (byte)0x4A,
      });

      subject.current_round = 14;
      subject.add_round_key();
      expect(subject.state, new byte[] {
        (byte)0, (byte)0x16, (byte)0x63, (byte)0xDB,
        (byte)0xAC, (byte)0x8b, (byte)0xAF, (byte)0xD6,
        (byte)0x55, (byte)0x27, (byte)0x10, (byte)0x2B,
        (byte)0x34, (byte)0x44, (byte)0xBE, (byte)0xCF,
      });
    }
  }

  //https://www.samiam.org/key-schedule.html
  public static class KeyScheduleTest extends FocusedTest {
    public String label() {
      return "It can generate the key schedule";
    }
    public void test() throws TestFailureException {
      AES256IGE subject = new AES256IGE();
      subject.generate_key_schedule(new byte[] {
        (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0
      });
      expect(subject.key_schedule, new byte[] {
        (byte)0, (byte)0, (byte)0, (byte)0,
          (byte)0, (byte)0, (byte)0, (byte)0,
          (byte)0, (byte)0, (byte)0, (byte)0,
          (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0, (byte)0, (byte)0, (byte)0,
          (byte)0, (byte)0, (byte)0, (byte)0,
          (byte)0, (byte)0, (byte)0, (byte)0,
          (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)0x62, (byte)0x63, (byte)0x63, (byte)0x63,
          (byte)0x62, (byte)0x63, (byte)0x63, (byte)0x63,
          (byte)0x62, (byte)0x63, (byte)0x63, (byte)0x63,
          (byte)0x62, (byte)0x63, (byte)0x63, (byte)0x63,
        (byte)0xaa, (byte)0xfb, (byte)0xfb, (byte)0xfb,
          (byte)0xaa, (byte)0xfb, (byte)0xfb, (byte)0xfb,
          (byte)0xaa, (byte)0xfb, (byte)0xfb, (byte)0xfb,
          (byte)0xaa, (byte)0xfb, (byte)0xfb, (byte)0xfb,
        (byte)0x6f, (byte)0x6c, (byte)0x6c, (byte)0xcf,
          (byte)0x0d, (byte)0x0f, (byte)0x0f, (byte)0xac,
          (byte)0x6f, (byte)0x6c, (byte)0x6c, (byte)0xcf,
          (byte)0x0d, (byte)0x0f, (byte)0x0f, (byte)0xac,
        (byte)0x7d, (byte)0x8d, (byte)0x8d, (byte)0x6a,
          (byte)0xd7, (byte)0x76, (byte)0x76, (byte)0x91,
          (byte)0x7d, (byte)0x8d, (byte)0x8d, (byte)0x6a,
          (byte)0xd7, (byte)0x76, (byte)0x76, (byte)0x91,
        (byte)0x53, (byte)0x54, (byte)0xed, (byte)0xc1,
          (byte)0x5e, (byte)0x5b, (byte)0xe2, (byte)0x6d,
          (byte)0x31, (byte)0x37, (byte)0x8e, (byte)0xa2,
          (byte)0x3c, (byte)0x38, (byte)0x81, (byte)0x0e,
        (byte)0x96, (byte)0x8a, (byte)0x81, (byte)0xc1,
          (byte)0x41, (byte)0xfc, (byte)0xf7, (byte)0x50,
          (byte)0x3c, (byte)0x71, (byte)0x7a, (byte)0x3a,
          (byte)0xeb, (byte)0x07, (byte)0x0c, (byte)0xab,
        (byte)0x9e, (byte)0xaa, (byte)0x8f, (byte)0x28,
          (byte)0xc0, (byte)0xf1, (byte)0x6d, (byte)0x45,
          (byte)0xf1, (byte)0xc6, (byte)0xe3, (byte)0xe7,
          (byte)0xcd, (byte)0xfe, (byte)0x62, (byte)0xe9,
        (byte)0x2b, (byte)0x31, (byte)0x2b, (byte)0xdf,
          (byte)0x6a, (byte)0xcd, (byte)0xdc, (byte)0x8f,
          (byte)0x56, (byte)0xbc, (byte)0xa6, (byte)0xb5,
          (byte)0xbd, (byte)0xbb, (byte)0xaa, (byte)0x1e,
        (byte)0x64, (byte)0x06, (byte)0xfd, (byte)0x52,
          (byte)0xa4, (byte)0xf7, (byte)0x90, (byte)0x17,
          (byte)0x55, (byte)0x31, (byte)0x73, (byte)0xf0,
          (byte)0x98, (byte)0xcf, (byte)0x11, (byte)0x19,
        (byte)0x6d, (byte)0xbb, (byte)0xa9, (byte)0x0b,
          (byte)0x07, (byte)0x76, (byte)0x75, (byte)0x84,
          (byte)0x51, (byte)0xca, (byte)0xd3, (byte)0x31,
          (byte)0xec, (byte)0x71, (byte)0x79, (byte)0x2f,
        (byte)0xe7, (byte)0xb0, (byte)0xe8, (byte)0x9c,
          (byte)0x43, (byte)0x47, (byte)0x78, (byte)0x8b,
          (byte)0x16, (byte)0x76, (byte)0x0b, (byte)0x7b,
          (byte)0x8e, (byte)0xb9, (byte)0x1a, (byte)0x62,
        (byte)0x74, (byte)0xed, (byte)0x0b, (byte)0xa1,
          (byte)0x73, (byte)0x9b, (byte)0x7e, (byte)0x25,
          (byte)0x22, (byte)0x51, (byte)0xad, (byte)0x14,
          (byte)0xce, (byte)0x20, (byte)0xd4, (byte)0x3b,
        (byte)0x10, (byte)0xf8, (byte)0x0a, (byte)0x17,
          (byte)0x53, (byte)0xbf, (byte)0x72, (byte)0x9c,
          (byte)0x45, (byte)0xc9, (byte)0x79, (byte)0xe7,
          (byte)0xcb, (byte)0x70, (byte)0x63, (byte)0x85
      });
    }
  }
}
