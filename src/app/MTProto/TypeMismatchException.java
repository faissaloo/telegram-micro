package mtproto;
import java.lang.RuntimeException;
import java.lang.Throwable;

public class TypeMismatchException extends RuntimeException {
  public TypeMismatchException(String message) {
    super(message);
  }
}
