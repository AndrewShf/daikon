

package utilMDE;
import java.util.*;

// *****
// Automatically generated from MathMDE-cpp.java
// *****

/** Mathematical utilities. */
public final class MathMDE {

  ///
  /// Function versions of Java operators
  ///

  public static int negate(int a) {
    return -a;
  }

  public static int bitwiseComplement(int a) {
    return ~a;
  }

  public static int mul(int x, int y) {
    return x * y;
  }

  public static int div(int x, int y) {
    return x / y;
  }

  public static int mod(int x, int y) {
    return x % y;
  }

  public static int lshift(int x, int y) {
    return x << y;
  }

  public static int rshiftSigned(int x, int y) {
    return x >> y;
  }

  public static int rshiftUnsigned(int x, int y) {
    return x >>> y;
  }

  public static int bitwiseAnd(int x, int y) {
    return x & y;
  }

  public static int logicalAnd(int x, int y) {
    return ((x!=0) & (y!=0)) ? 1 : 0;
  }

  public static int bitwiseXor(int x, int y) {
    return x ^ y;
  }

  public static int logicalXor(int x, int y) {
    return ((x!=0) ^ (y!=0)) ? 1 : 0;
  }

  public static int bitwiseOr(int x, int y) {
    return x | y;
  }

  public static int logicalOr(int x, int y) {
    return ((x!=0) | (y!=0)) ? 1 : 0;
  }

  ///
  /// negation
  ///

  public static int sign(int a) {
    if (a==0)
      return 0;
    else if (a>0)
      return 1;
    else
      return -1;
  }

  ///
  /// exponentiation
  ///

  /**
   * Returns of value of the first argument raised to the
   * power of the second argument.
   * @see{Math.pow(double, double)}
   */
  public static int pow(int base, int expt) throws ArithmeticException {
    return pow_fast(base, expt);
  }

  public static int pow_fast(int base, int expt) throws ArithmeticException {
    if (expt < 0)
      throw new ArithmeticException("Negative base passed to pow");

    int this_square_pow = base;
    int result = 1;
    while (expt>0) {
      if ((expt & 1) != 0)
	result *= this_square_pow;
      expt >>= 1;
      this_square_pow *= this_square_pow;
    }
    return result;
  }

  public static int pow_slow(int base, int expt) throws ArithmeticException {
    if (expt < 0)
      throw new ArithmeticException("Negative base passed to pow");

    int result = 1;
    for (int i=0; i<expt; i++)
      result *= base;
    return result;
  }

  ///
  /// gcd
  ///

  /** Return the greatest common divisor of the two arguments. */
  public static int  gcd(int  a, int  b) {
    if (b == 0)
      return 0;
    while (b != 0) {
      int  tmp = b;
      b = a % b;
      a = tmp;
    }
    return a;
  }

  /** Return the greatest common divisor of the elements of int  array a. */
  public static int  gcd(int [] a) {
    if (a.length == 0) {
      return 0;
    }
    int  result = a[0];
    for (int i=1; i<a.length; i++) {
      result = gcd(a[i], result);
      if ((result == 1) || (result == 0))
	return result;
    }
    return result;
  }

  /**
   * Return the gcd (greatest common divisor) of the differences
   * between the elements of int  array a.
   */
  public static int  gcd_differences(int [] a) {
    if (a.length < 2) {
      return 0;
    }
    int  result = a[1] - a[0];
    for (int i=2; i<a.length; i++) {
      result = gcd(a[i] - a[i-1], result);
      if ((result == 1) || (result == 0))
	return result;
    }
    return result;
  }

  // gcd -- version for manipulating long (rather than int) values

  /** Return the greatest common divisor of the two arguments. */
  public static long  gcd(long  a, long  b) {
    if (b == 0)
      return 0;
    while (b != 0) {
      long  tmp = b;
      b = a % b;
      a = tmp;
    }
    return a;
  }

  /** Return the greatest common divisor of the elements of long  array a. */
  public static long  gcd(long [] a) {
    if (a.length == 0) {
      return 0;
    }
    long  result = a[0];
    for (int i=1; i<a.length; i++) {
      result = gcd(a[i], result);
      if ((result == 1) || (result == 0))
	return result;
    }
    return result;
  }

  /**
   * Return the gcd (greatest common divisor) of the differences
   * between the elements of long  array a.
   */
  public static long  gcd_differences(long [] a) {
    if (a.length < 2) {
      return 0;
    }
    long  result = a[1] - a[0];
    for (int i=2; i<a.length; i++) {
      result = gcd(a[i] - a[i-1], result);
      if ((result == 1) || (result == 0))
	return result;
    }
    return result;
  }

  ///
  /// Modulus
  ///



  /** Return z such that (z == x mod y) and (0 <= z < abs(y)). */
  public static final int  mod_positive(int  x, int  y) {
    int  result = x % y;
    if (result < 0)
      result += Math.abs(y);
    return result;
  }

  /**
   * Return an array of two integers (r,m) such that each number in NUMS is equal to r (mod m).
   * The largest possible modulus is used, and the trivial constraint that all
   * integers are equal to 0 mod 1 is not returned (null is returned instead).
   */
  public static int [] modulus(int [] nums) {
    if (nums.length < 3)
    return null;

    int  modulus = Math.abs(gcd_differences(nums));
    if (modulus == 1)
      return null;

    int  remainder = nums[0] % modulus;
    if (remainder < 0)
      remainder += modulus;

    return new int [] { remainder, modulus };
  }

  /**
   * The iterator produces Integer  values.
   * This can be more efficient than modulus(int []) if the int [] doesn't already
   * exist, because this does not necessarily examine every value produced by
   * its iterator.
   */
  public static int [] modulus_int (Iterator itor) {
    if (!itor.hasNext())
      return null;
    int  avalue = ((Integer )itor.next()). intValue ();
    if (!itor.hasNext())
      return null;
    int  modulus = Math.abs(avalue - ((Integer )itor.next()). intValue ());
    if (modulus == 1)
      return null;
    int count = 2;
    while (itor.hasNext()) {
      int  i = ((Integer )itor.next()). intValue ();
      if (i == avalue)
	continue;
      modulus = MathMDE.gcd(modulus, Math.abs(avalue - i));
      count++;
      if (modulus == 1)
	return null;
      }
    if (count < 3)
      return null;
    return new int [] { MathMDE.mod_positive(avalue, modulus), modulus } ;
  }

  /**
   * Return an array of two integers (r,m) such that each number in NUMS is equal to r (mod m).
   * The largest possible modulus is used, and the trivial constraint that all
   * integers are equal to 0 mod 1 is not returned (null is returned instead).
   *
   * This "_strict" version requires its input to be sorted, and no element
   * may be missing.
   */
  public static int [] modulus_strict(int [] nums) {
    if (nums.length < 3)
    return null;

    int  modulus = nums[1] - nums[0];
    if (modulus == 1)
      return null;
    for (int i=2; i<nums.length; i++)
      if (nums[i] - nums[i-1] != modulus)
	return null;

    return new int [] { mod_positive(nums[0], modulus), modulus };
  }

  /**
   * The iterator produces Integer  values.
   * This can be more efficient than modulus(int []) if the int [] doesn't already
   * exist, because this does not necessarily examine every value produced by
   * its iterator.
   *
   * This "_strict" version requires its input to be sorted, and no element
   * may be missing.
   */
  public static int [] modulus_strict_int (Iterator itor) {
    if (!itor.hasNext())
      return null;
    int  prev = ((Integer )itor.next()). intValue ();
    if (!itor.hasNext())
      return null;
    int  next = ((Integer )itor.next()). intValue ();
    int  modulus = next-prev;
    if (modulus == 1)
      return null;
    int count = 2;
    while (itor.hasNext()) {
      prev = next;
      next = ((Integer )itor.next()). intValue ();
      if (next - prev != modulus)
	return null;
      count++;
    }
    if (count < 3)
      return null;
    return new int [] { MathMDE.mod_positive(next, modulus), modulus } ;
  }

  // modulus for long (as opposed to int) values



  /** Return z such that (z == x mod y) and (0 <= z < abs(y)). */
  public static final long  mod_positive(long  x, long  y) {
    long  result = x % y;
    if (result < 0)
      result += Math.abs(y);
    return result;
  }

  /**
   * Return an array of two integers (r,m) such that each number in NUMS is equal to r (mod m).
   * The largest possible modulus is used, and the trivial constraint that all
   * integers are equal to 0 mod 1 is not returned (null is returned instead).
   */
  public static long [] modulus(long [] nums) {
    if (nums.length < 3)
    return null;

    long  modulus = Math.abs(gcd_differences(nums));
    if (modulus == 1)
      return null;

    long  remainder = nums[0] % modulus;
    if (remainder < 0)
      remainder += modulus;

    return new long [] { remainder, modulus };
  }

  /**
   * The iterator produces Long  values.
   * This can be more efficient than modulus(long []) if the long [] doesn't already
   * exist, because this does not necessarily examine every value produced by
   * its iterator.
   */
  public static long [] modulus_long (Iterator itor) {
    if (!itor.hasNext())
      return null;
    long  avalue = ((Long )itor.next()). longValue ();
    if (!itor.hasNext())
      return null;
    long  modulus = Math.abs(avalue - ((Long )itor.next()). longValue ());
    if (modulus == 1)
      return null;
    int count = 2;
    while (itor.hasNext()) {
      long  i = ((Long )itor.next()). longValue ();
      if (i == avalue)
	continue;
      modulus = MathMDE.gcd(modulus, Math.abs(avalue - i));
      count++;
      if (modulus == 1)
	return null;
      }
    if (count < 3)
      return null;
    return new long [] { MathMDE.mod_positive(avalue, modulus), modulus } ;
  }

  /**
   * Return an array of two integers (r,m) such that each number in NUMS is equal to r (mod m).
   * The largest possible modulus is used, and the trivial constraint that all
   * integers are equal to 0 mod 1 is not returned (null is returned instead).
   *
   * This "_strict" version requires its input to be sorted, and no element
   * may be missing.
   */
  public static long [] modulus_strict(long [] nums) {
    if (nums.length < 3)
    return null;

    long  modulus = nums[1] - nums[0];
    if (modulus == 1)
      return null;
    for (int i=2; i<nums.length; i++)
      if (nums[i] - nums[i-1] != modulus)
	return null;

    return new long [] { mod_positive(nums[0], modulus), modulus };
  }

  /**
   * The iterator produces Long  values.
   * This can be more efficient than modulus(long []) if the long [] doesn't already
   * exist, because this does not necessarily examine every value produced by
   * its iterator.
   *
   * This "_strict" version requires its input to be sorted, and no element
   * may be missing.
   */
  public static long [] modulus_strict_long (Iterator itor) {
    if (!itor.hasNext())
      return null;
    long  prev = ((Long )itor.next()). longValue ();
    if (!itor.hasNext())
      return null;
    long  next = ((Long )itor.next()). longValue ();
    long  modulus = next-prev;
    if (modulus == 1)
      return null;
    int count = 2;
    while (itor.hasNext()) {
      prev = next;
      next = ((Long )itor.next()). longValue ();
      if (next - prev != modulus)
	return null;
      count++;
    }
    if (count < 3)
      return null;
    return new long [] { MathMDE.mod_positive(next, modulus), modulus } ;
  }

  ///
  /// Non-Modulus
  ///



  /**
   * Return an array containing all the numbers *not* in its argument array
   * but in the argument's range; that is, bigger than its argument's
   * minimum value and smaller than its argument's maximum value.
   * The result contains no duplicates and is in order.
   */
  public static int [] missing_numbers(int [] nums) {
    Arrays.sort(nums);
    int  min = nums[0];
    int  max = nums[nums.length-1];
    int [] result = new int [ max - min + 1 - nums.length  ];
    int result_index = 0;
    int  val = min;
    for (int i=0; i<nums.length; i++) {
      while (val < nums[i]) {
	result[result_index] = val;
	result_index++;
	val++;
      }
      if (val == nums[i]) {
	val++;
      }
    }
    if (result_index == result.length) {
      return result;
    } else {
      // There were duplicates in the nums array, so we didn't fill up
      // the result array.
      int [] new_result = new int [result_index];
      System.arraycopy(result, 0, new_result, 0, result_index);
      return new_result;
    }
  }

  /**
   * This iterator returns all the numbers *not* in its argument array
   * but in the argument's range; that is, bigger than its argument's
   * minimum value and smaller than its argument's maximum value.
   * The result contains no duplicates and is in order.
   */
  static final class MissingNumbersIteratorInt  implements Iterator {
    int [] nums;
    Iterator nums_itor;
    int  current_nonmissing;
    int  current_missing;
    int current_index;

    MissingNumbersIteratorInt (int [] nums) {
      Arrays.sort(nums);
      current_index = 0;
      current_nonmissing = nums[current_index];
      current_missing = current_nonmissing;
      this.nums = nums;
    }

    // The iterator must return the Integers in sorted order
    MissingNumbersIteratorInt (Iterator nums_itor) {
      if (!nums_itor.hasNext())
	throw new Error("No elements in nums_itor");
      current_nonmissing = ((Integer )nums_itor.next()). intValue ();
      current_missing = current_nonmissing;
      this.nums_itor = nums_itor;
    }

    public boolean hasNext() {
      if (current_missing < current_nonmissing)
	return true;
      // This loop ("while" instead of "if") permits duplicates in nums.
      while (current_missing == current_nonmissing) {
	if (nums != null) {
	  current_index++;
	  if (current_index >= nums.length)
	    return false;
	  current_nonmissing = nums[current_index];
	} else if (nums_itor != null) {
	  if (!nums_itor.hasNext())
	    return false;
	  // prev_nonmissing is for testing only
	  int  prev_nonmissing = current_nonmissing;
	  current_nonmissing = ((Integer )nums_itor.next()). intValue ();
	  if (! (prev_nonmissing < current_nonmissing))
	    throw new Error("Non-sorted Iterator supplied to MissingNumbersIteratorINT: prev_nonmissing = " + prev_nonmissing + ", current_nonmissing = " + current_nonmissing);
	} else {
	  throw new Error("Can't happen");
	}
	current_missing++;
	return hasNext();
      }
      throw new Error("Can't happen: " + current_missing + " " + current_nonmissing);
    }

    public Object next() {
      if (!hasNext())
	throw new NoSuchElementException();
      Integer  result = new Integer (current_missing);
      current_missing++;
      return result;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Return a tuple of (r,m) where no number in NUMS is equal to r (mod
   * m) but all missing numbers in their range are.
   */
  public static int [] nonmodulus_strict(int [] nums) {
    // This implementation is particularly inefficient; find a better way to
    // compute this.  Perhaps obtain the new modulus numbers incrementally
    // instead of all at once.
    if (nums.length == 0)
      return null;
    int  range = ArraysMDE.element_range(nums);
    if (range > 65536)
      return null;
    // Must not use regular modulus:  that can produce errors, eg
    // nonmodulus_strict({1,2,3,5,6,7,9,11}) => {0,2}.
    return modulus_strict_int (new MissingNumbersIteratorInt (nums));
  }

  /**
   * Return a tuple of (r,m) where no number in NUMS is equal to r (mod
   * m) but all missing numbers in their range are.
   */
  public static int [] nonmodulus_strict_int (Iterator nums) {
    // Must not use regular modulus:  that can produce errors, eg
    // nonmodulus_strict({1,2,3,5,6,7,9,11}) => {0,2}.
    return modulus_strict_int (new MissingNumbersIteratorInt (nums));
  }

  // Old, slightly less efficient implementation that uses the version of
  // missing_numbers that returns an array instead of an Iterator.
  // /**
  //  * Return a tuple of (r,m) where no number in NUMS is equal to r (mod
  //  * m) but all missing numbers in their range are.
  //  */
  // public static int [] nonmodulus_strict(int [] nums) {
  //   // This implementation is particularly inefficient; find a better way to
  //   // compute this.  Perhaps obtain the new modulus numbers incrementally
  //   // instead of all at once.
  //   if (nums.length == 0)
  //     return null;
  //   int  range = ArraysMDE.element_range(nums);
  //   if (range > 65536)
  //     return null;
  //   return modulus(missing_numbers(nums));
  // }

  /**
   * Return a tuple of (r,m) where no number in NUMS is equal to r (mod m)
   * but for every number in NUMS, at least one is equal to every non-r remainder.
   * The modulus is chosen as small as possible, but no greater than half the
   * range of the input numbers (else null is returned).
   */
  // This seems to give too many false positives (or maybe my probability
  // model was wrong); use nonmodulus_strict instead.
  public static int [] nonmodulus_nonstrict(int [] nums) {
    if (nums.length < 4)
      return null;
    int max_modulus =  Math.min(nums.length/2, ArraysMDE.element_range(nums)/2)  ;

    // System.out.println("nums.length=" + nums.length + ", range=" + ArraysMDE.element_range(nums) + ", max_modulus=" + max_modulus);

    // no real sense checking 2, as common_modulus would have found it, but
    // include it to make this function stand on its own
    for (int m=2; m<=max_modulus; m++) {
      // System.out.println("Trying m=" + m);
      boolean[] has_modulus = new boolean[m]; // initialized to false?
      int num_nonmodulus = m;
      for (int i=0; i<nums.length; i++) {
	int rem =  mod_positive(nums[i], m)  ;
	if (!has_modulus[rem]) {
	  has_modulus[rem] = true;
	  num_nonmodulus--;
	  // System.out.println("rem=" + rem + " for " + nums[i] + "; num_nonmodulus=" + num_nonmodulus);
	  if (num_nonmodulus == 0)
	    // Quit as soon as we see every remainder instead of processing
	    // each element of the input list.
	    break;
	}
      }
      // System.out.println("For m=" + m + ", num_nonmodulus=" + num_nonmodulus);
      if (num_nonmodulus == 1) {
	return new int [] {ArraysMDE.indexOf(has_modulus, false), m};
      }
    }
    return null;
  }

  // non-modulus for long (as opposed to int) values



  /**
   * Return an array containing all the numbers *not* in its argument array
   * but in the argument's range; that is, bigger than its argument's
   * minimum value and smaller than its argument's maximum value.
   * The result contains no duplicates and is in order.
   */
  public static long [] missing_numbers(long [] nums) {
    Arrays.sort(nums);
    long  min = nums[0];
    long  max = nums[nums.length-1];
    long [] result = new long [new Long( max - min + 1 - nums.length ).intValue() ];
    int result_index = 0;
    long  val = min;
    for (int i=0; i<nums.length; i++) {
      while (val < nums[i]) {
	result[result_index] = val;
	result_index++;
	val++;
      }
      if (val == nums[i]) {
	val++;
      }
    }
    if (result_index == result.length) {
      return result;
    } else {
      // There were duplicates in the nums array, so we didn't fill up
      // the result array.
      long [] new_result = new long [result_index];
      System.arraycopy(result, 0, new_result, 0, result_index);
      return new_result;
    }
  }

  /**
   * This iterator returns all the numbers *not* in its argument array
   * but in the argument's range; that is, bigger than its argument's
   * minimum value and smaller than its argument's maximum value.
   * The result contains no duplicates and is in order.
   */
  static final class MissingNumbersIteratorLong  implements Iterator {
    long [] nums;
    Iterator nums_itor;
    long  current_nonmissing;
    long  current_missing;
    int current_index;

    MissingNumbersIteratorLong (long [] nums) {
      Arrays.sort(nums);
      current_index = 0;
      current_nonmissing = nums[current_index];
      current_missing = current_nonmissing;
      this.nums = nums;
    }

    // The iterator must return the Integers in sorted order
    MissingNumbersIteratorLong (Iterator nums_itor) {
      if (!nums_itor.hasNext())
	throw new Error("No elements in nums_itor");
      current_nonmissing = ((Long )nums_itor.next()). longValue ();
      current_missing = current_nonmissing;
      this.nums_itor = nums_itor;
    }

    public boolean hasNext() {
      if (current_missing < current_nonmissing)
	return true;
      // This loop ("while" instead of "if") permits duplicates in nums.
      while (current_missing == current_nonmissing) {
	if (nums != null) {
	  current_index++;
	  if (current_index >= nums.length)
	    return false;
	  current_nonmissing = nums[current_index];
	} else if (nums_itor != null) {
	  if (!nums_itor.hasNext())
	    return false;
	  // prev_nonmissing is for testing only
	  long  prev_nonmissing = current_nonmissing;
	  current_nonmissing = ((Long )nums_itor.next()). longValue ();
	  if (! (prev_nonmissing < current_nonmissing))
	    throw new Error("Non-sorted Iterator supplied to MissingNumbersIteratorINT: prev_nonmissing = " + prev_nonmissing + ", current_nonmissing = " + current_nonmissing);
	} else {
	  throw new Error("Can't happen");
	}
	current_missing++;
	return hasNext();
      }
      throw new Error("Can't happen: " + current_missing + " " + current_nonmissing);
    }

    public Object next() {
      if (!hasNext())
	throw new NoSuchElementException();
      Long  result = new Long (current_missing);
      current_missing++;
      return result;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Return a tuple of (r,m) where no number in NUMS is equal to r (mod
   * m) but all missing numbers in their range are.
   */
  public static long [] nonmodulus_strict(long [] nums) {
    // This implementation is particularly inefficient; find a better way to
    // compute this.  Perhaps obtain the new modulus numbers incrementally
    // instead of all at once.
    if (nums.length == 0)
      return null;
    long  range = ArraysMDE.element_range(nums);
    if (range > 65536)
      return null;
    // Must not use regular modulus:  that can produce errors, eg
    // nonmodulus_strict({1,2,3,5,6,7,9,11}) => {0,2}.
    return modulus_strict_long (new MissingNumbersIteratorLong (nums));
  }

  /**
   * Return a tuple of (r,m) where no number in NUMS is equal to r (mod
   * m) but all missing numbers in their range are.
   */
  public static long [] nonmodulus_strict_long (Iterator nums) {
    // Must not use regular modulus:  that can produce errors, eg
    // nonmodulus_strict({1,2,3,5,6,7,9,11}) => {0,2}.
    return modulus_strict_long (new MissingNumbersIteratorLong (nums));
  }

  // Old, slightly less efficient implementation that uses the version of
  // missing_numbers that returns an array instead of an Iterator.
  // /**
  //  * Return a tuple of (r,m) where no number in NUMS is equal to r (mod
  //  * m) but all missing numbers in their range are.
  //  */
  // public static long [] nonmodulus_strict(long [] nums) {
  //   // This implementation is particularly inefficient; find a better way to
  //   // compute this.  Perhaps obtain the new modulus numbers incrementally
  //   // instead of all at once.
  //   if (nums.length == 0)
  //     return null;
  //   long  range = ArraysMDE.element_range(nums);
  //   if (range > 65536)
  //     return null;
  //   return modulus(missing_numbers(nums));
  // }

  /**
   * Return a tuple of (r,m) where no number in NUMS is equal to r (mod m)
   * but for every number in NUMS, at least one is equal to every non-r remainder.
   * The modulus is chosen as small as possible, but no greater than half the
   * range of the input numbers (else null is returned).
   */
  // This seems to give too many false positives (or maybe my probability
  // model was wrong); use nonmodulus_strict instead.
  public static long [] nonmodulus_nonstrict(long [] nums) {
    if (nums.length < 4)
      return null;
    int max_modulus = new Long( Math.min(nums.length/2, ArraysMDE.element_range(nums)/2) ).intValue() ;

    // System.out.println("nums.length=" + nums.length + ", range=" + ArraysMDE.element_range(nums) + ", max_modulus=" + max_modulus);

    // no real sense checking 2, as common_modulus would have found it, but
    // include it to make this function stand on its own
    for (int m=2; m<=max_modulus; m++) {
      // System.out.println("Trying m=" + m);
      boolean[] has_modulus = new boolean[m]; // initialized to false?
      int num_nonmodulus = m;
      for (int i=0; i<nums.length; i++) {
	int rem = new Long( mod_positive(nums[i], m) ).intValue() ;
	if (!has_modulus[rem]) {
	  has_modulus[rem] = true;
	  num_nonmodulus--;
	  // System.out.println("rem=" + rem + " for " + nums[i] + "; num_nonmodulus=" + num_nonmodulus);
	  if (num_nonmodulus == 0)
	    // Quit as soon as we see every remainder instead of processing
	    // each element of the input list.
	    break;
	}
      }
      // System.out.println("For m=" + m + ", num_nonmodulus=" + num_nonmodulus);
      if (num_nonmodulus == 1) {
	return new long [] {ArraysMDE.indexOf(has_modulus, false), m};
      }
    }
    return null;
  }

}
