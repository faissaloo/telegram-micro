package support;

import java.util.Vector;

public class Queue {
  Vector vector;

  public Queue() {
    vector = new Vector();
  }

  public void enqueue(Object object) {
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
}
