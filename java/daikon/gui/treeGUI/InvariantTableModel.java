package daikon.gui.treeGUI;

import java.util.*;
import java.text.DecimalFormat;
import javax.swing.table.*;
import utilMDE.Assert;
import daikon.inv.Invariant;
import daikon.inv.filter.InvariantFilters;

//  Each Ppt table is associated with a tableModel, which controls what information is
//  displayed (which invariants, and what data from the invariants), as well as how the
//  information is displayed (column headings, etc).  When the user changes the filter
//  settings, this class recomputes which invariants should be displayed.

class InvariantTableModel extends AbstractTableModel {
  static final String[] columnNames = { "invariant", "# values", "# samples", "probability", "justified" };
  static final Class[] columnClasses = { String.class, Integer.class, Integer.class, Double.class, Boolean.class };
  static final DecimalFormat format = new DecimalFormat( "0.##E0" ); // for displaying probabilities

  List allInvariants;
  List filteredInvariants;	// only filtered invariants are displayed

  public InvariantTableModel( List invariants, InvariantFilters invariantFilters ) {
    allInvariants = invariants;
    updateInvariantList( invariantFilters );
  }

  public int getRowCount() { return filteredInvariants.size(); }

  public int getColumnCount() { return columnNames.length; }

  public String getColumnName( int column ) { return columnNames[ column ]; }

  public Object getValueAt( int row, int column ) {
    Assert.assertTrue( column >= 0  &&  column <= 4 );
    Invariant invariant = (Invariant) filteredInvariants.get( row );
    if (column == 0)        return invariant.format();
    else if (column == 1)           return new Double(Double.NaN); // [INCR] invariant.ppt.num_values()
    else if (column == 2)           return new Integer( invariant.ppt.num_samples());
    else if (column == 3)           return new Double( format.format( Math.round( 100 * invariant.getProbability()) / 100.0 ));
    else /* (column == 4) */        return new Boolean( invariant.justified());
  }

  //  Must override this method so TableSorter will sort numerical columns properly.
  public Class getColumnClass( int column ) {
    return columnClasses[ column ];
  }

  public void updateInvariantList( InvariantFilters invariantFilters ) {
    filteredInvariants = new ArrayList();
    for (Iterator iter = allInvariants.iterator(); iter.hasNext(); ) {
      Invariant invariant = (Invariant) iter.next();
      if (invariantFilters.shouldKeep( invariant ) == null)
	filteredInvariants.add( invariant );
    }
    filteredInvariants = InvariantFilters.addEqualityInvariants( filteredInvariants );

    fireTableDataChanged();
  }
}
