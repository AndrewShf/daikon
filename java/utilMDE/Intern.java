package utilMDE;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Utilities for interning objects.
 **/
public final class Intern {

  ///////////////////////////////////////////////////////////////////////////
  /// Strings
  ///

  /**
   * Replace each element of the array by its interned version.
   * @see String#intern
   **/
  public static void internStrings(String[] a) {
    for (int i=0; i<a.length; i++)
      if (a[i] != null)
        a[i] = a[i].intern();
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Testing interning
  ///

  /**
   * Return true if the argument is interned (is canonical among all
   * objects equal to itself).
   **/
  public static boolean isInterned(Object value) {
    if (value == null) {
      // nothing to do
      return true;
    } else if (value instanceof String) {
      return (value == ((String) value).intern());
    } else if (value instanceof String[]) {
      return (value == intern((String[]) value));
    } else if (value instanceof Integer) {
      return (value == intern((Integer) value));
    } else if (value instanceof Long) {
      return (value == intern((Long) value));
    } else if (value instanceof int[]) {
      return (value == intern((int[]) value));
    } else if (value instanceof long[]) {
      return (value == intern((long[]) value));
    } else if (value instanceof Double) {
      return (value == intern((Double) value));
    } else if (value instanceof double[]) {
      return (value == intern((double[]) value));
    } else if (value instanceof Object[]) {
      return (value == intern((Object[]) value));
    } else {
      // Nothing to do, because we don't intern other types.
      // System.out.println("What type? " + value.getClass().getName());
      return true;
    }
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Interning objects
  ///

  /**
   * Hasher object which hashes and compares Integers.
   * This is the obvious implementation that uses intValue() for the hashCode.
   * @see Hasher
   **/
  private static final class IntegerHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return a1.equals(a2);
    }
    public int hashCode(Object o) {
      Integer i = (Integer) o;
      return i.intValue();
    }
  }

  /**
   * Hasher object which hashes and compares Longs.
   * This is the obvious implementation that uses intValue() for the hashCode.
   * @see Hasher
   **/
  private static final class LongHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return a1.equals(a2);
    }
    public int hashCode(Object o) {
      Long i = (Long) o;
      return i.intValue();
    }
  }

  /**
   * Hasher object which hashes and compares int[] objects according
   * to their contents.
   * @see Hasher, java.util.Arrays.equals
   **/
  private static final class IntArrayHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return java.util.Arrays.equals((int[])a1, (int[])a2);
    }
    public int hashCode(Object o) {
      int[] a = (int[])o;
      int result = 0;
      for (int i=0; i<a.length; i++) {
        result = result * FACTOR + a[i];
      }
      return result;
    }
  }

  /**
   * Hasher object which hashes and compares long[] objects according
   * to their contents.
   * @see Hasher, java.util.Arrays.equals
   **/
  private static final class LongArrayHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return java.util.Arrays.equals((long[])a1, (long[])a2);
    }
    public int hashCode(Object o) {
      long[] a = (long[])o;
      long result = 0;
      for (int i=0; i<a.length; i++) {
        result = result * FACTOR + a[i];
      }
      return (int) (result % Integer.MAX_VALUE);
    }
  }

  private final static int FACTOR = 23;
  // private static final double DOUBLE_FACTOR = 65537;
  private final static double DOUBLE_FACTOR = 263;

  /**
   * Hasher object which hashes and compares Doubles.
   * @see Hasher
   **/
  private static final class DoubleHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return a1.equals(a2);
    }
    public int hashCode(Object o) {
      Double d = (Double) o;
      // Could add "... % Integer.MAX_VALUE" here; is that good to do?
      long result = Math.round(d.doubleValue() * DOUBLE_FACTOR);
      return (int) (result % Integer.MAX_VALUE);
    }
  }

  /**
   * Hasher object which hashes and compares double[] objects according
   * to their contents.
   * @see Hasher, java.util.Arrays.equals
   **/
  private static final class DoubleArrayHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      // "java.util.Arrays.equals" considers +0.0 != -0.0.
      // Also, it gives inconsistent results (on different JVMs/classpaths?).
      // return java.util.Arrays.equals((double[])a1, (double[])a2);
      double[] da1 = (double[])a1;
      double[] da2 = (double[])a2;
      if (da1.length != da2.length)
        return false;
      for (int i=0; i<da1.length; i++) {
        if (! ((da1[i] == da2[i])
               || (Double.isNaN(da1[i]) && Double.isNaN(da2[i])))) {
          return false;
        }
      }
      return true;
    }
    public int hashCode(Object o) {
      double[] a = (double[])o;
      double running = 0;
      for (int i=0; i<a.length; i++) {
        double elt = (Double.isNaN(a[i]) ? 0.0 : a[i]);
        running = running * FACTOR + elt * DOUBLE_FACTOR;
      }
      // Could add "... % Integer.MAX_VALUE" here; is that good to do?
      long result = Math.round(running);
      return (int) (result % Integer.MAX_VALUE);
    }
  }

  /**
   * Hasher object which hashes and compares String[] objects according
   * to their contents.
   * @see Hasher, java.util.Arrays.equals
   **/
  private static final class StringArrayHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return java.util.Arrays.equals((String[])a1, (String[])a2);
    }
    public int hashCode(Object o) {
      String[] a = (String[])o;
      int result = 0;
      for (int i=0; i<a.length; i++) {
        int a_hashcode = (a[i] == null) ? 0 : a[i].hashCode();
        result = result * FACTOR + a_hashcode;
      }
      return result;
    }
  }

  /**
   * Hasher object which hashes and compares Object[] objects according
   * to their contents.
   * @see Hasher, java.util.Arrays.equals
   **/
  private static final class ObjectArrayHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return java.util.Arrays.equals((Object[])a1, (Object[])a2);
    }
    public int hashCode(Object o) {
      Object[] a = (Object[])o;
      int result = 0;
      for (int i=0; i<a.length; i++) {
        int a_hashcode = (a[i] == null) ? 0 : a[i].hashCode();
        result = result * FACTOR + a_hashcode;
      }
      return result;
    }
  }

  // Map from an ArrayWrapper to the array (I don't need to map to a
  // WeakReference because the array isn't the key of the WeakHashMap).

  private static WeakHasherMap internedIntegers;
  private static WeakHasherMap internedLongs;
  private static WeakHasherMap internedIntArrays;
  private static WeakHasherMap internedLongArrays;
  private static WeakHasherMap internedDoubles;
  private static Double internedDoubleNaN;
  private static Double internedDoubleZero;
  private static WeakHasherMap internedDoubleArrays;
  private static WeakHasherMap internedStringArrays;
  private static WeakHasherMap internedObjectArrays;
  private static WeakHasherMap internedSequenceAndIndices;

  static {
    internedIntegers = new WeakHasherMap(new IntegerHasher());
    internedLongs = new WeakHasherMap(new LongHasher());
    internedIntArrays = new WeakHasherMap(new IntArrayHasher());
    internedLongArrays = new WeakHasherMap(new LongArrayHasher());
    internedDoubles = new WeakHasherMap(new DoubleHasher());
    internedDoubleNaN = new Double(Double.NaN);
    internedDoubleZero = new Double(0);
    internedDoubleArrays = new WeakHasherMap(new DoubleArrayHasher());
    internedStringArrays = new WeakHasherMap(new StringArrayHasher());
    internedObjectArrays = new WeakHasherMap(new ObjectArrayHasher());
    internedSequenceAndIndices = new WeakHasherMap(new SequenceAndIndicesHasher());
  }

  // For testing only
  public static int numIntegers() { return internedIntegers.size(); }
  public static int numLongs() { return internedLongs.size(); }
  public static int numIntArrays() { return internedIntArrays.size(); }
  public static int numLongArrays() { return internedLongArrays.size(); }
  public static int numDoubles() { return internedDoubles.size(); }
  public static int numDoubleArrays() { return internedDoubleArrays.size(); }
  public static int numStringArrays() { return internedStringArrays.size(); }
  public static int numObjectArrays() { return internedObjectArrays.size(); }
  public static Iterator integers() { return internedIntegers.keySet().iterator(); }
  public static Iterator longs() { return internedLongs.keySet().iterator(); }
  public static Iterator intArrays() { return internedIntArrays.keySet().iterator(); }
  public static Iterator longArrays() { return internedLongArrays.keySet().iterator(); }
  public static Iterator doubles() { return internedDoubles.keySet().iterator(); }
  public static Iterator doubleArrays() { return internedDoubleArrays.keySet().iterator(); }
  public static Iterator stringArrays() { return internedStringArrays.keySet().iterator(); }
  public static Iterator objectArrays() { return internedObjectArrays.keySet().iterator(); }

  // Interns a String.
  // Delegates to the builtin String.intern() method.  Provided for
  // completeness, so we can intern() any type used in OneOf.java.jpp.
  public static String intern(String a) {
    return (a == null) ? null : a.intern();
  }

  // Interns a long.
  // A no-op.  Provided for completeness, so we can intern() any type
  // used in OneOf.java.jpp.
  public static long intern(long l) {
    return l;
  }

  // Interns a long.
  // A no-op.  Provided for completeness, so we can intern() any type
  // used in OneOf.java.jpp.
  public static double intern(double l) {
    return l;
  }

  /**
   * Intern (canonicalize) an Integer.
   * Returns a canonical representation for the Integer.
   **/
  public static Integer intern(Integer a) {
    Object lookup = internedIntegers.get(a);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (Integer)ref.get();
    } else {
      internedIntegers.put(a, new WeakReference(a));
      return a;
    }
  }

  // Not sure whether this convenience method is really worth it.
  /** Returns an interned Integer with value i. */
  public static Integer internedInteger(int i) {
    return intern(new Integer(i));
  }

  // Not sure whether this convenience method is really worth it.
  /** Returns an interned Integer with value parsed from the string. */
  public static Integer internedInteger(String s) {
    return intern(Integer.decode(s));
  }


  /**
   * Intern (canonicalize) a Long.
   * Returns a canonical representation for the Long.
   **/
  public static Long intern(Long a) {
    Object lookup = internedLongs.get(a);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (Long)ref.get();
    } else {
      internedLongs.put(a, new WeakReference(a));
      return a;
    }
  }

  // Not sure whether this convenience method is really worth it.
  /** Returns an interned Long with value i. */
  public static Long internedLong(long i) {
    return intern(new Long(i));
  }

  // Not sure whether this convenience method is really worth it.
  /** Returns an interned Long with value parsed from the string. */
  public static Long internedLong(String s) {
    return intern(Long.decode(s));
  }


  // I might prefer to have the intern methods first check using a straight
  // eq hashing, which would be more efficient if the array is already
  // interned.  (How frequent do I expect that to be, and how much would
  // that really improve performance even in that case?)

  /**
   * Intern (canonicalize) an int[].
   * Returns a canonical representation for the int[] array.
   * Arrays are compared according to their elements.
   **/
  public static int[] intern(int[] a) {
    Object lookup = internedIntArrays.get(a);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (int[])ref.get();
    } else {
      internedIntArrays.put(a, new WeakReference(a));
      return a;
    }
  }

  /**
   * Intern (canonicalize) a long[].
   * Returns a canonical representation for the long[] array.
   * Arrays are compared according to their elements.
   **/
  public static long[] intern(long[] a) {
    Object lookup = internedLongArrays.get(a);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (long[])ref.get();
    } else {
      internedLongArrays.put(a, new WeakReference(a));
      return a;
    }
  }

  /**
   * Intern (canonicalize) a Double.
   * Returns a canonical representation for the Double.
   **/
  public static Double intern(Double a) {
    // Double.NaN == Double.Nan  always evaluates to false.
    if (a.isNaN())
      return internedDoubleNaN;
    // Double.+0 == Double.-0,  but they compare true via equals()
    if (a.doubleValue() == 0)   // catches both positive and negative zero
      return internedDoubleZero;
    Object lookup = internedDoubles.get(a);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (Double)ref.get();
    } else {
      internedDoubles.put(a, new WeakReference(a));
      return a;
    }
  }

  // Not sure whether this convenience method is really worth it.
  /** Returns an interned Double with value i. */
  public static Double internedDouble(double d) {
    return intern(new Double(d));
  }

  // Not sure whether this convenience method is really worth it.
  /** Returns an interned Double with value parsed from the string. */
  public static Double internedDouble(String s) {
    return internedDouble(Double.parseDouble(s));
  }


  // I might prefer to have the intern methods first check using a straight
  // eq hashing, which would be more efficient if the array is already
  // interned.  (How frequent do I expect that to be, and how much would
  // that really improve performance even in that case?)

  /**
   * Intern (canonicalize) a double[].
   * Returns a canonical representation for the double[] array.
   * Arrays are compared according to their elements.
   **/
  public static double[] intern(double[] a) {
    Object lookup = internedDoubleArrays.get(a);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (double[])ref.get();
    } else {
      internedDoubleArrays.put(a, new WeakReference(a));
      return a;
    }
  }

  /**
   * Intern (canonicalize) an String[].
   * Returns a canonical representation for the String[] array.
   * Arrays are compared according to their elements.
   * The elements should themselves already be interned;
   * they are compared using their equals() methods.
   **/
  public static String[] intern(String[] a) {

    // Make sure each element is already interned
    if (Assert.enabled) {
      for (int k = 0; k < a.length; k++)
        Assert.assertTrue (a[k] == Intern.intern (a[k]));
    }

    Object lookup = internedStringArrays.get(a);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (String[])ref.get();
    } else {
      internedStringArrays.put(a, new WeakReference(a));
      return a;
    }
  }

  /**
   * Intern (canonicalize) an Object[].
   * Returns a canonical representation for the Object[] array.
   * Arrays are compared according to their elements.
   * The elements should themselves already be interned;
   * they are compared using their equals() methods.
   **/
  public static Object[] intern(Object[] a) {
    Object lookup = internedObjectArrays.get(a);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (Object[])ref.get();
    } else {
      internedObjectArrays.put(a, new WeakReference(a));
      return a;
    }
  }


  /**
   * Convenince method to intern an Object when we don't know its
   * runtime type.  Its runtime type must be one of the types for
   * which we have an intern() method, else an exception is thrown.
   **/
  public static Object intern(Object a) {
    if (a == null) {
      return null;
    } else if (a instanceof String) {
      return intern((String) a);
    } else if (a instanceof String[]) {
      return intern((String[]) a);
    } else if (a instanceof Integer) {
      return intern((Integer) a);
    } else if (a instanceof Long) {
      return intern((Long) a);
    } else if (a instanceof int[]) {
      return intern((int[]) a);
    } else if (a instanceof long[]) {
      return intern((long[]) a);
    } else if (a instanceof Double) {
      return intern((Double) a);
    } else if (a instanceof double[]) {
      return intern((double[]) a);
    } else if (a instanceof Object[]) {
      return intern((Object[]) a);
    } else {
      throw new IllegalArgumentException
        ("Arguments of type " + a.getClass() + " cannot be interned");
    }
  }

  /**
   * Return the subsequence of seq from start (inclusive) to end
   * (exclusive) that is interned.  What's different about this method
   * from manually findind the subsequence and interning the
   * subsequence is that if the subsequence is already interned, we
   * can avoid having to compute the sequence.  Since derived
   * variables in Daikon compute the subsequence many times, this
   * shortcut saves quite a bit of computation.  It saves even more
   * when there may be many derived variables that are non canonical,
   * since they are guaranteed to be ==.
   * <p>
   * Requires that seq is already interned
   * @return a subsequence of seq from start to end that is interned.
   **/
  public static int[] internSubsequence (int[] seq, int start, int end) {
    Assert.assertTrue (Intern.isInterned(seq));
    SequenceAndIndices sai = new SequenceAndIndices (seq, start, end);
    Object lookup = internedSequenceAndIndices.get(sai);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (int[])ref.get();
    } else {
      int[] subseq = ArraysMDE.subarray(seq, start, end - start);
      subseq = Intern.intern (subseq);
      internedSequenceAndIndices.put (sai, new WeakReference(subseq));
      return subseq;
    }
  }

  /**
   * @see #internSubsequence(int[], int, int)
   **/
  public static long[] internSubsequence (long[] seq, int start, int end) {
    Assert.assertTrue (Intern.isInterned(seq));
    SequenceAndIndices sai = new SequenceAndIndices (seq, start, end);
    Object lookup = internedSequenceAndIndices.get(sai);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (long[])ref.get();
    } else {
      long[] subseq = ArraysMDE.subarray(seq, start, end - start);
      subseq = Intern.intern (subseq);
      internedSequenceAndIndices.put (sai, new WeakReference(subseq));
      return subseq;
    }
  }

  /**
   * @see #internSubsequence(int[], int, int)
   **/
  public static double[] internSubsequence (double[] seq, int start, int end) {
    Assert.assertTrue (Intern.isInterned(seq));
    SequenceAndIndices sai = new SequenceAndIndices (seq, start, end);
    Object lookup = internedSequenceAndIndices.get(sai);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (double[])ref.get();
    } else {
      double[] subseq = ArraysMDE.subarray(seq, start, end - start);
      subseq = Intern.intern (subseq);
      internedSequenceAndIndices.put (sai, new WeakReference(subseq));
      return subseq;
    }
  }

  /**
   * @see #internSubsequence(int[], int, int)
   **/
  public static Object[] internSubsequence (Object[] seq, int start, int end) {
    Assert.assertTrue (Intern.isInterned(seq));
    SequenceAndIndices sai = new SequenceAndIndices (seq, start, end);
    Object lookup = internedSequenceAndIndices.get(sai);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (Object[])ref.get();
    } else {
      Object[] subseq = ArraysMDE.subarray(seq, start, end - start);
      subseq = Intern.intern (subseq);
      internedSequenceAndIndices.put (sai, new WeakReference(subseq));
      return subseq;
    }
  }

  /**
   * @see #internSubsequence(int[], int, int)
   **/
  public static String[] internSubsequence (String[] seq, int start, int end) {
    Assert.assertTrue (Intern.isInterned(seq));
    SequenceAndIndices sai = new SequenceAndIndices (seq, start, end);
    Object lookup = internedSequenceAndIndices.get(sai);
    if (lookup != null) {
      WeakReference ref = (WeakReference)lookup;
      return (String[])ref.get();
    } else {
      String[] subseq = ArraysMDE.subarray(seq, start, end - start);
      subseq = Intern.intern (subseq);
      internedSequenceAndIndices.put (sai, new WeakReference(subseq));
      return subseq;
    }
  }

  /**
   * Data structure for storing triples of a sequence and start and
   * end indices, to represent a subsequence.  Requires that the
   * sequence be interned.  Used for interning the repeated finding
   * of subsequences on the same sequence.
   **/
  private static final class SequenceAndIndices {
    public Object seq;
    public int start;
    public int end;

    /**
     * @param seq An interned array
     **/
    public SequenceAndIndices (Object seq, int start, int end) {
      this.seq = seq;
      this.start = start;
      this.end = end;
    }

    public boolean equals (SequenceAndIndices other) {
      return (this.seq == other.seq) &&
        this.start == other.start &&
        this.end == other.end;
    }

    public int hashCode() {
      return seq.hashCode() + start * 30 - end * 2;
    }
  }

  /**
   * Hasher object which hashes and compares String[] objects according
   * to their contents.
   * @see Hasher, java.util.Arrays.equals
   *
   **/
  private static final class SequenceAndIndicesHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return ((SequenceAndIndices) a1).equals ((SequenceAndIndices) a2);
    }

    public int hashCode(Object o) {
      return o.hashCode();
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Interning arrays:  old implementation #1
  ///

  /// Interning arrays:  old implmentation.
  /// The problem with this is that it doesn't release keys.
  // // I can also use java.util.Arrays.equals() to compare two arrays of base
  // // or Object type; but that doesn't do ordering.  (It does properly deal
  // // with the possibility that the argument is null, which this doesn't
  // // right now.  I may want to err in this implementation if the arguments
  // // are null or the lengths are not equal -- if I never mix arrays of
  // // different lengths.)

  // // Note: this comparator imposes orderings that are inconsistent with equals.
  // // That is, it may return 0 if the arrays are not equal (but do contain
  // // identical numbers).
  // static final class IntArrayComparator implements Comparator {
  //   public int compare(Object o1, Object o2) {
  //     if (o1 == o2)
  //       return 0;
  //     int[] a1 = (int[])o1;
  //     int[] a2 = (int[])o2;
  //     int tmp;
  //     tmp = a1.length - a2.length;
  //     if (tmp != 0)
  //       return tmp;
  //     for (int i=0; i<a1.length; i++) {
  //       tmp = a1[i] - a2[i];
  //       if (tmp != 0)
  //         return tmp;
  //     }
  //     return 0;
  //   }
  // }

  // // Note: this comparator imposes orderings that are inconsistent with equals.
  // // That is, it may return 0 if the arrays are not equal (but do contain
  // // identical objects).
  // static final class ObjectArrayComparator implements Comparator {
  //   public int compare(Object o1, Object o2) {
  //     if (o1 == o2)
  //       return 0;
  //     Object[] a1 = (Object[])o1;
  //     Object[] a2 = (Object[])o2;
  //     int tmp;
  //     tmp = a1.length - a2.length;
  //     if (tmp != 0)
  //       return tmp;
  //     for (int i=0; i<a1.length; i++) {
  //       tmp = a1[i].hashCode() - a2[i].hashCode();
  //       if (tmp != 0)
  //         return tmp;
  //       // I'm counting on the fact that hashCode returns a different
  //       // number for each Object in the system.  This checks that assumption.
  //       Assert.assertTrue(a1[i].equals(a2[i]));
  //     }
  //     return 0;
  //   }
  // }

  // private static TreeSet internedIntArrays;
  // private static TreeSet internedObjectArrays;

  // static {
  //   internedIntArrays = new TreeSet(new IntArrayComparator());
  //   internedObjectArrays = new TreeSet(new ObjectArrayComparator());
  // }

  // public static int[] internIntArray(int[] a) {
  //   boolean added = internedIntArrays.add(a);
  //   if (added)
  //     return a;
  //   else
  //     return (int[])internedIntArrays.tailSet(a).first();
  // }

  // // All the elements should already themselves be interned
  // public static Object[] internObjectArray(Object[] a) {
  //   boolean added = internedObjectArrays.add(a);
  //   if (added)
  //     return a;
  //   else
  //     return (Object[])internedObjectArrays.tailSet(a).first();
  // }



  ///////////////////////////////////////////////////////////////////////////
  /// Interning arrays:  old implementation #2
  ///

  /// This doesn't work because there are no references to the Wrappers,
  /// so all of the WeakHashMap elements are immediately removed.

  // // Create an ArrayWrapper which redefines equal (and hash) to act the
  // // way I want them to.

  // static final class IntArrayWrapper {
  //   private int[] a;
  //   IntArrayWrapper(int[] a) {
  //     this.a = a;
  //   }
  //   boolean equals(IntArrayWrapper other) {
  //     return java.util.Arrays.equals(a, other.a);
  //   }
  //   static final int FACTOR = 23;
  //   public int hashCode() {
  //     int result = 0;
  //     for (int i=0; i<a.length; i++) {
  //       result = result * FACTOR + a[i];
  //     }
  //     return result;
  //   }
  // }

  // static final class ObjectArrayWrapper {
  //   private Object[] a;
  //   ObjectArrayWrapper(Object[] a) {
  //     this.a = a;
  //   }
  //   boolean equals(ObjectArrayWrapper other) {
  //     return java.util.Arrays.equals(a, other.a);
  //   }
  //   static final int FACTOR = 23;
  //   // Alternately, just xor all the element hash codes.
  //   public int hashCode() {
  //     int result = 0;
  //     for (int i=0; i<a.length; i++) {
  //       result = result * FACTOR + a[i].hashCode();
  //     }
  //     return result;
  //   }
  // }

  // // Map from an ArrayWrapper to the array (I don't need to map to a
  // // WeakReference because the array isn't the key of the WeakHashMap).

  // // non-private for debugging only
  // static WeakHashMap internedIntArrays;
  // static WeakHashMap internedObjectArrays;
  // // private static WeakHashMap internedIntArrays;
  // // private static WeakHashMap internedObjectArrays;

  // static {
  //   internedIntArrays = new WeakHashMap();
  //   internedObjectArrays = new WeakHashMap();
  // }

  // public static int[] internIntArray(int[] a) {
  //   IntArrayWrapper w = new IntArrayWrapper(a);
  //   Object result = internedIntArrays.get(w);
  //   if (result != null)
  //     return (int[])result;
  //   else {
  //     internedIntArrays.put(w, a);
  //     return a;
  //   }
  // }

  // // All the elements should already themselves be interned
  // public static Object[] internObjectArray(Object[] a) {
  //   ObjectArrayWrapper w = new ObjectArrayWrapper(a);
  //   Object result = internedObjectArrays.get(w);
  //   if (result != null)
  //     return (Object[])result;
  //   else {
  //     internedObjectArrays.put(w, a);
  //     return a;
  //   }
  // }

}
