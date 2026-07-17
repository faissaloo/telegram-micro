package support;

import java.util.Vector;

public class Queue {
  Vector vector;
  int capacity;

  public Queue() {
    this(Integer.MAX_VALUE);
  }

  public Queue(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("Queue capacity must be positive.");
    }
    vector = new Vector();
    this.capacity = capacity;
  }

  public void enqueue(Object object) {
    if (vector.size() >= capacity) {
      throw new IllegalStateException("Queue capacity exceeded.");
    }
    vector.addElement(object);
  }

  public Object dequeue() {
    Object dequeued_object = vector.firstElement();
    vector.removeElementAt(0);
    return dequeued_object;
  }

  public int length() {
    return vector.size();
  }

  public void clear() {
    vector.removeAllElements();
  }
}
