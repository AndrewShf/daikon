package daikon;

import daikon.inv.*;

import utilMDE.*;

import java.util.*;


// This is a fake PptSlice for use with Implication invariants.

// - The implication invariants at a program point are grouped into a
// single PptSlice0 with no variables

// - In order to output pre-state invariants as if they were
// post-state, or OBJECT invariants as if they applied to a particular
// parameter, we construct a PptSlice0 whose VarInfos have had their
// names tweaked, and temporarily use that as the invariant's ppt.

public class PptSlice0
  extends PptSlice
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  PptSlice0(PptTopLevel parent) {
     super(parent, new VarInfo[0]);
  }

  public final int arity() {
    return 0;
  }

  // Make a fake slice whose variables are the same as the ones in
  // sliceTemplate, but marked as prestate (i.e., orig(x) rather than x).
  public static PptSlice makeFakePrestate(PptSlice sliceTemplate) {
    PptSlice0 fake = new PptSlice0(sliceTemplate.parent);
    fake.var_infos = new VarInfo[sliceTemplate.var_infos.length];
    for (int i=0; i < fake.var_infos.length; i++) {
      fake.var_infos[i] = VarInfo.origVarInfo(sliceTemplate.var_infos[i]);
    }
    return fake;
  }

  // Make a fake slice whose variables are the same as the ones in
  // sliceTemplate, but with "this" replaced by newName.
  public static PptSlice makeFakeReplaceThis(PptSlice template,
                                             VarInfoName newName) {
    PptSlice0 fake = new PptSlice0(template.parent);
    fake.var_infos = new VarInfo[template.var_infos.length];
    for (int i=0; i < fake.var_infos.length; i++) {
      VarInfo svi = template.var_infos[i];
      fake.var_infos[i] =
        new VarInfo(svi.name.replace(VarInfoName.THIS, newName),
                    svi.type, svi.file_rep_type,
                    svi.comparability.makeAlias(svi.name), svi.aux);
    }
    return fake;
  }

  // We trade space for time by keeping a hash table of all the
  // implications (they're also stored as a vector in invs) so we can
  // efficiently avoid adding implications more than once.

  // Really a HashSet<ImplicationByFormatWrapper>.
  // This should not be transient:  more implications can be created during
  // printing, for instance due to guarding.
  private transient HashSet invariantsSeen = new HashSet();

  // In lieu of a readResolve method.
  private void initInvariantsSeen() {
    if (invariantsSeen == null) {
      invariantsSeen = new HashSet();
      for (Iterator itor = invs.iterator(); itor.hasNext(); ) {
        Implication inv = (Implication) itor.next();
        invariantsSeen.add(new ImplicationByFormatWrapper(inv));
      }
    }
  }

  void init_po() {
    throw new Error("Shouldn't get called");
  }

  public void checkRep() {
    if (invariantsSeen != null && invs.size() != invariantsSeen.size()) {
      Assert.assertTrue(invs.size() == invariantsSeen.size(),
                        "invs.size()=" + invs.size() + ", invariantsSeen.size()=" + invariantsSeen.size());
    }
    Assert.assertTrue(invariantsSeen == null || invs.size() == invariantsSeen.size());
  }

  /**
   * The invariant is typically an Implication; but PptSlice0 can contain other joiners than than implications,
   * such as "and" or "or".  That feature isn't used as of November 2003.
   **/
  public void addInvariant(Invariant inv) {
    Assert.assertTrue(inv != null);
    Assert.assertTrue(inv instanceof Implication);
    // checkRep();
    // Assert.assertTrue(! hasImplication((Implication) inv));
    initInvariantsSeen();
    invs.add(inv);
    invariantsSeen.add(new ImplicationByFormatWrapper((Implication)inv));
    // checkRep();
  }

  public void removeInvariant(Invariant inv) {
    Assert.assertTrue(inv != null);
    Assert.assertTrue(inv instanceof Implication);
    // checkRep();
    // Assert.assertTrue(hasImplication((Implication) inv));
    initInvariantsSeen();
    invs.remove(inv);
    invariantsSeen.remove(new ImplicationByFormatWrapper((Implication)inv));
    // checkRep();
  }

  // This can be called with very long lists by the conditionals code.
  // At least until that's fixed, it's important for it not to be
  // quadratic.
  public void removeInvariants(List to_remove) {
    if (to_remove.size() < 10) {
      for (int i=0; i<to_remove.size(); i++) {
        removeInvariant((Invariant) to_remove.get(i));
      }
    } else {
      invs.removeMany(to_remove);
      if (to_remove.size() > invariantsSeen.size() / 2) {
        // Faster to throw away and recreate
        invariantsSeen = null;
        initInvariantsSeen();
      } else {
        // Faster to update
        for (int i=0; i<to_remove.size(); i++) {
          invariantsSeen.remove(new
              ImplicationByFormatWrapper((Implication)to_remove.get(i)));
        }
      }
    }
  }

  public boolean hasImplication(Implication imp) {
    initInvariantsSeen();
    return invariantsSeen.contains(new ImplicationByFormatWrapper(imp));
  }

  // // For debugging only
  // public Implication getImplication(Implication imp) {
  //   initInvariantsSeen();
  //   ImplicationByFormatWrapper resultWrapper
  //     = (ImplicationByFormatWrapper) UtilMDE.getFromSet(
  //              invariantsSeen, new ImplicationByFormatWrapper(imp));
  //   if (resultWrapper == null) {
  //     return null;
  //   }
  //   return (Implication) resultWrapper.theImp;
  // }


  // We'd like to use a more sophisticated equality check and hashCode
  // for implications when they appear in the invariantsSeen HashSet,
  // but not anywhere else, so we make wrapper objects with the
  // desired methods to go directly in the set.

  // Not "implements serializable":  If this is serializable, then the hash
  // set tries to get the hash codes of all the invariants when it
  // reads them in, but their format methods croak when they couldn't
  // get their varInfos.

  // It seems like a bit of a hack to use format() this way, but the
  // check this is replacing (used to be in makeImplication())
  // compared two invariants by their format() values, so I'm
  // assuming there's some good reason. -SMcC
  // Yes, it is a hack and should be fixed.  Note that there are certain
  // invariants that print identically but are internally different:
  // "this.theArray[this.topOfStack..] == this.theArray[this.topOfStack..]"
  // can be either SeqSeqIntEqual or PairwiseLinearBinary.  Thus, I changed
  // it from using format() to using repr(); but that doesn't fix the
  // underlying issue.

  private static final class ImplicationByFormatWrapper {

    public Implication theImp;
    // private String format;
    // hashCode is cached to make equality checks faster.
    private int hashCode;

    public ImplicationByFormatWrapper(Implication theImp) {
      this.theImp = theImp;
      // this.format = theImp.format();
      this.hashCode = 0;
    }

    // Abstracted out to permit use of a cached value
    private String format() {
      // return format;
      // return theImp.format();
      return theImp.repr();
    }

    public int hashCode() {
      if (hashCode == 0) {
        hashCode = format().hashCode();
        // hashCode = (theImp.iff ? 1 : 0);
        // hashCode = 37 * hashCode + theImp.predicate().getClass().hashCode();
        // hashCode = 37 * hashCode + theImp.consequent().getClass().hashCode();
      }
      return hashCode;
    }

    public boolean equals(Object o) {
      if (o == null)
        return false;
      Assert.assertTrue(o instanceof ImplicationByFormatWrapper);
      ImplicationByFormatWrapper other = (ImplicationByFormatWrapper)o;
      if (hashCode() != other.hashCode()) {
        return false;
      }
      // return format().equals(other.format());
      return theImp.isSameInvariant(other.theImp);
    }

  }

  // I need to figure out how to set these.
  public int num_samples() { return 2222; }
  public int num_mod_samples() { return 2222; }
  public int num_values() { return 2222; }

  void instantiate_invariants() {
    throw new Error("Shouldn't get called");
  }

  public List add(ValueTuple vt, int count) {
    throw new Error("Shouldn't get called");
  }

}
