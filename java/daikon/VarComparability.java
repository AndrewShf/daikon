package daikon;

import utilMDE.*;

import org.apache.log4j.Category;



// Internally, we use the names "array[]", "array[]-element", and
// "array[]-indexn".  These may be different depending on the programming
// language; for instance, C uses "*array" in place of "array[]-element".


/**
 * Represents the comparability of variables, including methods to
 * determine if two VarComparabilities are comparable.
 * VarComparability types have three formats: implicit, explicit, and none.<p>
 *
 * A VarComparabilityImplicit is an arbitrary string, and comparisons
 * succeed exactly if the two VarComparabilitys are identical.<p>
 *
 * A VarComparabilityExplicit is a list of other variable names, and
 * comparisons succeed if each variable is in the list of the other.<p>
 *
 * VarComparabilityNone means no comparability information was provided.<p>
 **/
public abstract class VarComparability {

  /** Debug tracer **/
  public static final Category debug =
    Category.getInstance ("daikon.VarComparability");


  public static final int NONE = 0;
  public static final int IMPLICIT = 1;
  public static final int EXPLICIT = 2;

  /**
   * Create a VarComparability representing the given arguments with
   * respect to a variable.
   * @param format the type of comparability, either NONE, IMPLICIT or EXPLICIT
   * @param rep if an explicit type, a regular expression indicating
   * how to match.  The form is "(a)[b][c]..." where each variable is
   * string (or number) that is a UID for a basic type.  a is the type
   * of the element, b is the type of the first index, c the type of
   * the second, etc.  Index variables only apply if this is an array.
   * @param vartype the declared type of the variable
   **/
  public static VarComparability parse(int format, String rep, ProglangType vartype) {
    if (format == NONE) {
      return VarComparabilityNone.parse(rep, vartype);
    } else if (format == IMPLICIT) {
      return VarComparabilityImplicit.parse(rep, vartype);
    } else if (format == EXPLICIT) {
      return VarComparabilityExplicit.parse(rep, vartype);
    } else {
      throw new Error("bad format argument " + format
                      + " should have been in {0, 1, 2}");
    }
  }

  /**
   * Create a VarComparability based on comparabilities of indices.
   * @return a new comparability that is an array with the same dimensionality
   * and indices as given, but with a different element type.
   *
   * @param elemTypeName the new type of the elements of return value.
   * @param old the varcomparability that this is derived from; has
   * the same indices as this.
   **/
  public static VarComparability makeComparabilitySameIndices (String elemTypeName,
                                                               VarComparability old) {
    if (old instanceof VarComparabilityExplicit) {
      String[] elems = new String[] {elemTypeName};
      Intern.internStrings (elems);
      return new VarComparabilityExplicit (elems, ((VarComparabilityExplicit) old).indices,
                                           ((VarComparabilityExplicit) old).dimensions,
                                           null);
    } else if (old instanceof VarComparabilityNone) {
      return VarComparabilityNone.it;
    } else {
      throw new Error ("Not implemented for implicity comparables");
    }
  }

  public static VarComparability makeAlias(VarInfo vi) {
    return vi.comparability.makeAlias(vi.name);
  }
  public abstract VarComparability makeAlias(VarInfoName name);

  public abstract VarComparability elementType();
  public abstract VarComparability indexType(int dim);


  /** Returns whether two variables are comparable **/
  public static boolean comparable(VarInfo v1, VarInfo v2) {
    return comparable(v1.name, v1.comparability, v2.name, v2.comparability);
  }

  /** Returns whether two comparabilities are comparable **/
  public static boolean comparable(VarComparability type1, VarComparability type2) {
    return comparable(null, type1, null, type2);
  }

  /** Returns whether two variables are comparable **/
  public static boolean comparable(VarInfoName name1, VarComparability type1,
                                   VarInfoName name2, VarComparability type2) {

    if (type1 != null && type2 != null && type1.getClass() != type2.getClass())
      throw new Error("Trying to compare VarComparabilities " +
                      "of different types: " + Global.lineSep
                      + "    " + name1 + " " + type1 + Global.lineSep
                      + "    " + name2 + " " + type2);

    if (type1 instanceof VarComparabilityNone || type1 == null || type2 == null) {
      return VarComparabilityNone.comparable
        (name1, (VarComparabilityNone)type1,
         name2, (VarComparabilityNone)type2);
    } else if (type1 instanceof VarComparabilityImplicit) {
        return VarComparabilityImplicit.comparable
          (name1, (VarComparabilityImplicit)type1,
           name2, (VarComparabilityImplicit)type2);
    } else if (type1 instanceof VarComparabilityExplicit) {
      return VarComparabilityExplicit.comparable
        (name1, (VarComparabilityExplicit)type1,
         name2, (VarComparabilityExplicit)type2);
    } else {
      throw new Error("Unrecognized subtype of VarComparability: " + type1);
    }
  }

}
