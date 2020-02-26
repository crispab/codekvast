package sample.app;

/**
 * This class is inside of the packagePrefix that the Codekvast agent monitors.
 *
 * @author olle.hallin@crisp.se
 */
public class TrackedClass extends AbstractTracked implements Tracked, Comparable<TrackedClass> {
  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public void setCount2(int count, int p2) {
    this.count = count;
  }

  @Override
  public int publicMethod() {
    return count++;
  }

  private int privateMethod() {
    return count++;
  }

  int packagePrivateMethod() {
    return count++;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TrackedClass that = (TrackedClass) o;

    return count == that.count;
  }

  @Override
  public int hashCode() {
    return count;
  }

  @Override
  public int compareTo(TrackedClass that) {
    return that.count - this.count;
  }

  @Override
  public String toString() {
    return "TrackedClass{count=" + count + '}';
  }
}
