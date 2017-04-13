package com.winterwell.maths.datastorage;

/**
 * A column which uses an index to map objects to numbers.
 * 
 * @author daniel
 */
public final class IndexedColumn<X> extends ColumnInfo<X> {

	private boolean addNewObjects;

	private IIndex<X> index;

	public IndexedColumn(String name, IIndex<X> index) {
		super(name);
		this.index = index;
	}

	@Override
	public X convertFromDouble(double x) {
		long i = Math.round(x);
		return index.get((int) i);
	}

	@Override
	public double convertToDouble(X x) {
		int i = addNewObjects ? index.indexOfWithAdd(x) : index.indexOf(x);
		if (i < 0)
			throw new IllegalArgumentException("Unrecognised: " + x);
		return i;
	}

	/**
	 * false by default. If true, previously unseen objects will be added to the
	 * index.
	 * 
	 * @param addNewObjects
	 */
	public void setAddNewObjects(boolean addNewObjects) {
		this.addNewObjects = addNewObjects;
	}
}
