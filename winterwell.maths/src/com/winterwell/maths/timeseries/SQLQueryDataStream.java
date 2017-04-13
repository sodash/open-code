/**
 *
 */
package com.winterwell.maths.timeseries;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * Wrap a JDBC ResultSet as a DataStream. All fields must be double (or
 * numerical?).
 * 
 * Status: Experimental
 * 
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class SQLQueryDataStream extends ADataStream {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final Connection db;
	final String query;

	/**
	 * @throws SQLException
	 */
	public SQLQueryDataStream(Connection db, String query, int dimension) {
		super(dimension);
		this.db = db;
		this.query = query;
	}

	@Override
	public IDataStream factory(Object sourceSpecifier)
			throws ClassCastException {
		String q2 = (String) sourceSpecifier;
		return new SQLQueryDataStream(db, q2, getDim());
	}

	@Override
	public boolean isFactory() {
		return true;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		try {
			Statement s = db.createStatement();
			s.setFetchSize(500);
			final ResultSet results = s.executeQuery(query);
			assert results.getMetaData().getColumnCount() == getDim();
			return new AbstractIterator<Datum>() {

				/*
				 * (non-Javadoc)
				 * 
				 * @see winterwell.utils.containers.AbstractIterable#getNext()
				 */
				@Override
				protected Datum next2() {
					try {
						boolean more = results.next();
						if (!more)
							return null;
						int dim = getDim();
						double x[] = new double[dim];
						for (int i = 0; i < dim; i++) {
							x[i] = results.getDouble(i + 1);
						}
						Datum result = new Datum(x);
						return result;
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
				}
			};
		} catch (SQLException e) {
			throw Utils.runtime(e);
		}
	}

}
