package crypto;
import support.RandomPlus;
import support.Debug;

public class SecureRandomPlus extends RandomPlus {
  RandomPlus random_number_generator;
  SHA256 hash_engine;
  byte[] generated_buffer;
  int buffer_index;
  
  public SecureRandomPlus() {
    super();
    hash_engine = new SHA256();
    random_number_generator = new RandomPlus();
    generate_buffer();
  }
  
  public void generate_buffer() {
    buffer_index = 0;
    generated_buffer = hash_engine.digest(random_number_generator.nextBytes(SHA256.HASH_SIZE));
  }
  
  public byte nextByte() {
    if (buffer_index >= generated_buffer.length-1) {
      generate_buffer();
    }
    buffer_index++;
    return generated_buffer[buffer_index];
  }
  
  protected int next(int bits) {    
    int bytes_needed = bits/8;
    int unmasked_bytes = 0;
    
    for (int i = 0; i < bytes_needed; i++) {
      unmasked_bytes <<= 8;
      unmasked_bytes |= nextByte();
    }
    
    int bit_mask = (1 << bits) - 1;
    return unmasked_bytes | bit_mask; // you idiot you utter buffoon, does this look correct to you? THis should be an &, hang your head in shame
    
  }
}
