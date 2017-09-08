package com.winterwell.datalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import com.winterwell.datalog.DataLog.KInterpolate;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.io.DBOptions;
import com.winterwell.utils.io.SqlUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;

/**
 * A PostgreSQL implementation of IStatStorage responsible for storing and retrieving stats to the database.
 *
 * The database connection parameters can be past via the StatConfig class. If null, then the default options
 * are used. A DB table, named "stats" is created when an object is initialised.
 * Connection and queries to/from the database are made using the methods in {@link SqlUtils}.
 *
 * FIXME This should use the database details from config, if they are set -- currently it only does this in save()
 * 
 * @testedby {@link SQLStorageTest}
 * @author Agis Chartsias <agis@winterwell.com>
 */
public class SQLStorage implements IDataLogStorage {

	int warnings;
	
	public static String TABLE = "stats";
	public static String COLUMNS_COUNT = " timestamp, tag, count_mean ";
	public static String COLUMNS_DISTR = " timestamp, tag, count_mean, variance, min, max ";
	
	private static int BATCH_SIZE = 100;
	
	private DataLogConfig config;

	/**
	 * FIXME: This constructor is called at sodash startup. It can't call initStatDB,
	 * because it calls {@link SqlUtils#setDBOptions(DBOptions)}. When sodash DB is initialising
	 * afterwards an assertion error is thrown.
	 * @DW Move initStatDB along with other DB inits?
	 *
	 * @param config
	 */
	public SQLStorage() {
	}
	
	@Override
	public IDataLogStorage init(DataLogConfig settings) {
		this.config = settings;
		initStatDB();
		return this;
	}
	

	private boolean initFlag;

	/**
	 * Check the db table exists and if not, create it.
	 * FIXME add "if not exists" if psql version > 9.1
	 */
	private void initStatDB() {
		if (initFlag) return;
		// ready?		
		if (SqlUtils.options == null && config.dbUrl==null) {
			// Not setup yet
			Log.w(DataLog.LOGTAG, "SQLStorage: Stat cannot access the DB yet: no DB url / login details");
			return;
		}		
		Log.w(DataLog.LOGTAG, "SQLStorage: init DB...");
		if (config.dbUrl!=null) {
			SqlUtils.setDBOptions(config);
		}

		initFlag = true;
		
		// Of course we expect all of the following to fail in the normal run of things
		SqlUtils.executeCommand("CREATE SEQUENCE stats_id_seq MINVALUE 0;", null, true);
		//SqlUtils.executeCommand("grant usage on sequence stats_id_seq to worker;", null, true); // I think unnecessary - JH

		String createsql = "create table stats " +
				"(id bigint not null default nextval('stats_id_seq'::regclass) primary key," +
				"timestamp bigint not null," +
				"tag text not null," +
				"count_mean float not null," +
				"variance float," +
				"min float," +
				"max float); ";
		SqlUtils.executeCommand(createsql, null, true);
		//SqlUtils.executeCommand("grant all privileges on stats to worker;", null, true); // I think unnecessary - JH

		// FIXME: The following can be removed after the #v13b rollout
		SqlUtils.executeCommand("alter index timestamp_index rename to stats_timestamp_idx;", null, true);
		SqlUtils.executeCommand("alter index tag_index rename to stats_tag_idx;", null, true);
		// Clean up some mess from an early indexing bug
		for(int i=1; i<50; i++) {
			try {
				boolean out = SqlUtils.executeCommand("drop INDEX stats_timestamp_idx"+i+";", null, false);
			} catch(Exception ex) {
				Log.d(DataLog.LOGTAG, "init.stat.cleanup "+ex); // hopefully all done
				break;
			}
		}
		
		SqlUtils.executeCommand("create index stats_timestamp_idx on stats (timestamp);", null, true);
		SqlUtils.executeCommand("create index stats_tag_idx on stats (tag);", null, true);
	}

	@Override
	public void registerEventType(String dataspace, String eventType) {
	}
	
	@Override
	public void save(Period period, Map<String, Double> tag2count, Map<String, MeanVar1D> tag2mean) {		
		initStatDB();
		if ( ! initFlag) {
			// Can't save!
			if (warnings < 3) {
				Log.e(DataLog.LOGTAG, "Cannot save! Database not initialised. Losing counts for: "+tag2count.keySet()+" "+tag2mean.keySet());
				warnings++;
			}			
			return;
		}
		assert initFlag;

		String sqlPrefix = "insert into " + TABLE;

		// Save as the middle of the period?!
		Time mid = DataLogImpl.doSave3_time(period);

		Connection conn = null;
		try {
			conn = config.dbUrl!=null? SqlUtils.getConnection(config) : SqlUtils.getConnection();
			for (String tag : tag2count.keySet()) {
				Double value = tag2count.get(tag);				
//				if (value!=null && value > 0) Log.d(Stat.LOGTAG, "saving " + tag + "=" + value + " to db");
	
				String values = " values (" + mid.getTime() + ", "+ SqlUtils.sqlEncode(tag) +", " + value + ");";
				String sql = sqlPrefix + " ( " + COLUMNS_COUNT + " ) " + values;
				SqlUtils.executeUpdate(sql, conn);
			}
	
			for (String tag : tag2mean.keySet()) {
				MeanVar1D value = tag2mean.get(tag);
//				Log.v(Stat.LOGTAG, "saving " + tag + "=" + value + " to db");
	
				String values = "values (" 	+
						mid.getTime() 			+ ", " +
						SqlUtils.sqlEncode(tag)	+ ", " +
						value.getMean() 		+ ", " +
						value.getVariance() 	+ ", " +
						value.getMin() 			+ ", " +
						value.getMax()			
//						value.getCount() TODO
						+ ");";
				String sql = sqlPrefix + " ( " + COLUMNS_DISTR + " ) " + values;

				SqlUtils.executeUpdate(sql, conn);
			}
		} finally {
			SqlUtils.close(conn);
		}
	}
	
	@Override
	public void saveHistory(Map<Pair2<String, Time>, Double> tag2time2count) {
		saveHistory2(tag2time2count, false);
	}
	
	/**
	 * Replaces values at the given tag/time pairs.
	 * @param tag2time2count
	 */
	@Override
	public void setHistory(Map<Pair2<String, Time>, Double> tag2time2count) {
		saveHistory2(tag2time2count, true);
	}
	
	/**
	 * Helper method for adding or setting a value in the db.
	 * 
	 * @param tag2time2count
	 * @param setOrAdd: if true the value replaces the old one, otherwise it adds to the
	 * old one if exists.
	 */
	private void saveHistory2(Map<Pair2<String, Time>, Double> tag2time2count, boolean setOrAdd) {
		initStatDB();
		if ( ! initFlag) {
			// Can't save!
			Log.e(DataLog.LOGTAG, "Cannot save history! Database not initialised. Losing counts for: "+tag2time2count.keySet());
			return;
		}
		if (tag2time2count.isEmpty()) return;
	
		int rowCount = 0;
		Connection conn = null;
		try {
			conn = config.dbUrl!=null? SqlUtils.getConnection(config) : SqlUtils.getConnection();
			// Try first an update and if it doesn't succeed insert the row.
			PreparedStatement select = conn.prepareStatement(
					"SELECT count_mean FROM " + TABLE + " WHERE timestamp = ? AND tag = ?;");			
			PreparedStatement update = conn.prepareStatement(
					"UPDATE " + TABLE + " SET " + " count_mean = ? WHERE timestamp = ? AND tag = ?;");
			PreparedStatement insert = conn.prepareStatement(
					"INSERT INTO " + TABLE + " (timestamp, count_mean, tag) values (?, ?, ?);");
			
			for (Pair2<String, Time> key : tag2time2count.keySet()) {
				long timestamp = key.second.getTime();
				String tag = key.first;
				Double value = tag2time2count.get(key);
				
				select.setDouble(1, timestamp);
				select.setString(2, tag);
				
				ResultSet rs = select.executeQuery();
				if (rs.next()) {
					// It already exists: update
					if ( ! setOrAdd) value += rs.getDouble(1);					
					update.setDouble(1, value);
					update.setLong(2, timestamp);
					update.setString(3, tag);
					update.addBatch();
				} else {
					// It doesn't exist: insert
					insert.setLong(1, timestamp);
					insert.setDouble(2, value);
					insert.setString(3, tag);
					insert.addBatch();
				}
								
				if (++rowCount % BATCH_SIZE == 0) {
					update.executeBatch();
					update.clearBatch();
					
					insert.executeBatch();
					insert.clearBatch();
					
					Log.d(DataLog.LOGTAG, "saving batch of " + BATCH_SIZE + " rows");
				}
			}
			
			// Final batch
			update.executeBatch();
			insert.executeBatch();
			conn.commit();
			Log.d(DataLog.LOGTAG, "saveHistory " + rowCount + " rows "+tag2time2count);			
		} catch (SQLException e) {
			Log.e(DataLog.LOGTAG, Utils.getRootCause(e));
			throw Utils.runtime(e);
		} finally {
			SqlUtils.close(conn);
		}
	}

	@Override
	public IFuture<IDataStream> getData(Pattern id, Time start, Time end) {
		return new StatReqSQL<IDataStream>(KStatReq.DATA, id, start, end);
	}

	@Override
	public StatReq<IDataStream> getData(String tag, Time start, Time end, KInterpolate fn, Dt bucketSize) {
		return new StatReqSQL<IDataStream>(KStatReq.DATA, tag, start, end, fn, bucketSize);
	}
	

	public IFuture<MeanRate> getMean(Time start, Time end, String tag) {
		return new StatReqSQL<MeanRate>(KStatReq.TOTAL, tag, start, end, null, null);		
	}
	
	@Override
	public StatReq<IDataStream> getMeanData(String tag, Time start, Time end, KInterpolate fn, Dt bucketSize) {
		return new StatReqSQL<IDataStream>(KStatReq.MEANDATA, tag, start, end, fn, bucketSize);
	}

	@Override
	public StatReq<Double> getTotal(String tag, Time start, Time end) {
		return new StatReqSQL<Double>(KStatReq.TOTAL, tag, start, end, null, null);
	}

	@Override
	public Iterator<Object[]> getReader(String server, Time s, Time e, Pattern tagMatcher, String tag) {
		initStatDB();
		String where = buildWhere(s, e, tagMatcher, tag);
		String order = " order by timestamp";
		String select = "select " + COLUMNS_DISTR + " from " + TABLE + where + order + ";";
		return SqlUtils.executeQuery(select, null, 0).iterator();
	}

	/**
	 *
	 * @param server
	 * @param s
	 * @param e
	 * @param tagMatcher
	 * @param tag
	 * @return the count of tags given the parameters.
	 */
	double selectSum(String server, Time s, Time e, Pattern tagMatcher, String tag) {
		initStatDB();
		String where = buildWhere(s, e, tagMatcher, tag);
		String select = "select sum(count_mean)" + " from " + TABLE + where + ";";

		Iterator<Object[]> itr = SqlUtils.executeQuery(select, null, 0).iterator();
		Object[] obj = itr.next();
		assert obj.length == 1 : "Invalid return object";
		assert !itr.hasNext() : "More sql results than expected.";

		if (obj[0] == null) return 0.0;

		return Double.parseDouble(obj[0].toString());
	}

	/**
	 * There are 3 approaches to pattern matching in PostgreSQL:
	 * 1. SQL "like" operator, which matches patterns of regular and wildcard characters. The pattern should match the entire string.
	 * 2. "similar to" operator, which is like "like", except that it uses SQL standard definition of a reg exp.
	 * 3. POSIX-style regexps.
	 * The following method uses approach (3). Works in most cases. Java does not support POSIX bracket expressions.
	 *
	 * @param s
	 * @param e
	 * @param tagMatcher Either this or tag must be set/null
	 * @param tag Either this or tagMatcher must be set/null
	 * @return the where clauses of the select statement.
	 */
	private String buildWhere(Time s, Time e, Pattern tagMatcher, String tag) {
		assert s != null : "Null start time";
		assert e != null : "Null end time";
		assert tagMatcher != null || tag != null : "One of tagMatcher/tag should not be null";
		assert tagMatcher == null || tag == null : "One of tagMatcher/tag should be null";

		// TODO should this be inclusive of start/end??
		String where = " where timestamp > " + s.getTime() + " and timestamp < " + e.getTime();
		if (tagMatcher != null) {
			assert SqlUtils.sqlEncode(tagMatcher.pattern()).equals(tagMatcher.pattern()) : "Need to escape pattern " + tagMatcher.pattern();
			String clause = " and tag ~ '" + tagMatcher.pattern() + "'";
			where += clause;
		}
		if (tag != null) {
			String clause = " and tag = " + SqlUtils.sqlEncode(tag);
			where += clause;
		}
		return where;
	}

	@Override
	public Object saveEvent(String dataspace, DataLogEvent event, Period period) {
		throw new TodoException();
	}


	@Override
	public void saveEvents(Collection<DataLogEvent> values, Period period) {
		// TODO Auto-generated method stub
		
	}
}

