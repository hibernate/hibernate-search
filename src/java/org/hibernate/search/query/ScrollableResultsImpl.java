//$Id$
package org.hibernate.search.query;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.search.SearchException;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.DocumentExtractor;
import org.hibernate.search.engine.EntityInfo;
import org.hibernate.search.engine.Loader;
import org.hibernate.type.Type;

/**
 * Implements scollable and paginated resultsets.
 * Contrary to query#iterate() or query#list(), this implementation is
 * exposed to returned null objects (if the index is out of date).
 * <p/>
 * <p/>
 * +  * The following methods that change the value of 'current' will check
 * +  * and set its value to either 'afterLast' or 'beforeFirst' depending
 * +  * on direction. This is to prevent rogue values from setting it outside
 * +  * the boundaries of the results.
 * +  * <ul>
 * +  * <li>next()</li>
 * +  * <li>previous()</li>
 * +  * <li>scroll(i)</li>
 * +  * <li>last()</li>
 * +  * <li>first()</li>
 * +  * </ul>
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 */
public class ScrollableResultsImpl implements ScrollableResults {
	private static Log log = LogFactory.getLog( ScrollableResultsImpl.class );
	private final IndexSearcher searcher;
	private final SearchFactory searchFactory;
	private final Hits hits;
	private final int first;
	private final int max;
	private final int fetchSize;
	private int current;
	private EntityInfo[] entityInfos;
	private Loader loader;
	private DocumentExtractor documentExtractor;
	private Map<EntityInfo, Object[]> resultContext;

	public ScrollableResultsImpl(
			IndexSearcher searcher, Hits hits, int first, int max, int fetchSize, DocumentExtractor extractor,
			Loader loader, SearchFactory searchFactory
	) {
		this.searcher = searcher;
		this.searchFactory = searchFactory;
		this.hits = hits;
		this.first = first;
		this.max = max;
		this.current = first;
		this.loader = loader;
		this.documentExtractor = extractor;
		int size = max - first + 1 > 0 ? max - first + 1 : 0;
		this.entityInfos = new EntityInfo[size];
		this.resultContext = new HashMap<EntityInfo, Object[]>( size );
		this.fetchSize = fetchSize;
	}

	// The 'cache' is a sliding window of size fetchSize that
	// moves back and forth over entityInfos as directed loading
	// values as necessary.
	private EntityInfo loadCache(int windowStart) {
		int windowStop;

		EntityInfo info = entityInfos[windowStart - first];
		if ( info != null ) {
			//data has already been loaded
			return info;
		}

		if ( windowStart + fetchSize > max ) {
			windowStop = max;
		}
		else {
			windowStop = windowStart + fetchSize - 1;
		}

		List<EntityInfo> entityInfosLoaded = new ArrayList<EntityInfo>( windowStop - windowStart + 1 );
		for (int x = windowStart; x <= windowStop; x++) {
			try {
				if ( entityInfos[x - first] == null ) {
					//FIXME should check that clazz match classes but this complicates a lot the firstResult/maxResult
					entityInfos[x - first] = documentExtractor.extract( hits, x );
					entityInfosLoaded.add( entityInfos[x - first] );
				}
			}
			catch (IOException e) {
				throw new HibernateException( "Unable to read Lucene hits[" + x + "]", e );
			}

		}
		//preload efficiently first
		loader.load( entityInfosLoaded.toArray( new EntityInfo[entityInfosLoaded.size()] ) );
		//load one by one to inject null results if needed
		for (EntityInfo slidingInfo : entityInfosLoaded) {
			if ( !resultContext.containsKey( slidingInfo ) ) {
				Object loaded = loader.load( slidingInfo );
				if ( !loaded.getClass().isArray() ) loaded = new Object[] { loaded };
				resultContext.put( slidingInfo, (Object[]) loaded );
			}
		}
		return entityInfos[windowStart - first];
	}

	/**
	 * Increases cursor pointer by one. If this places it >
	 * max + 1 (afterLast) then set it to afterLast and return
	 * false.
	 *
	 * @return booolean
	 * @throws HibernateException
	 */
	public boolean next() throws HibernateException {
		if ( ++current > max ) {
			afterLast();
			return false;
		}
		return true;
	}

	/**
	 * Decreases cursor pointer by one. If this places it <
	 * first - 1 (beforeFirst) then set it to beforeFirst and
	 * return false.
	 *
	 * @return boolean
	 * @throws HibernateException
	 */
	public boolean previous() throws HibernateException {
		if ( --current < first ) {
			beforeFirst();
			return false;
		}
		return true;
	}

	/**
	 * Since we have to take into account that we can scroll any
	 * amount positive or negative, we perform the same tests that
	 * we performed in next() and previous().
	 *
	 * @param i
	 * @return boolean
	 * @throws HibernateException
	 */
	public boolean scroll(int i) throws HibernateException {
		current = current + i;
		if ( current > max ) {
			afterLast();
			return false;
		}
		else if ( current < first ) {
			beforeFirst();
			return false;
		}
		else {
			return true;
		}
	}

	public boolean last() throws HibernateException {
		current = max;
		if ( current < first ) {
			beforeFirst();
			return false;
		}
		return max >= first;
	}

	public boolean first() throws HibernateException {
		current = first;
		if ( current > max ) {
			afterLast();
			return false;
		}
		return max >= first;
	}

	public void beforeFirst() throws HibernateException {
		current = first - 1;
	}

	public void afterLast() throws HibernateException {
		current = max + 1;
	}

	public boolean isFirst() throws HibernateException {
		return current == first;
	}

	public boolean isLast() throws HibernateException {
		return current == max;
	}

	public void close() throws HibernateException {
		try {
			searchFactory.getReaderProvider().closeReader( searcher.getIndexReader() );
		}
		catch (SearchException e) {
			log.warn( "Unable to properly close searcher in ScrollableResults", e );
		}
	}

	public Object[] get() throws HibernateException {
		// don't throw an exception here just
		// return 'null' this is similar to the
		// RowSet spec in JDBC. It returns false
		// (or 0 I can't remember) but we can't
		// do that since we have to make up for
		// an Object[]. J.G
		if ( current < first || current > max ) return null;
		loadCache( current );
		return resultContext.get( entityInfos[current - first] );
	}

	public Object get(int i) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Type getType(int i) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Integer getInteger(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Long getLong(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Float getFloat(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Boolean getBoolean(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Double getDouble(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Short getShort(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Byte getByte(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Character getCharacter(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public byte[] getBinary(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public String getText(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Blob getBlob(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Clob getClob(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public String getString(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public BigDecimal getBigDecimal(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public BigInteger getBigInteger(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Date getDate(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Locale getLocale(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public Calendar getCalendar(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public TimeZone getTimeZone(int col) throws HibernateException {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public int getRowNumber() throws HibernateException {
		if ( max < first ) return -1;
		return current - first;
	}

	public boolean setRowNumber(int rowNumber) throws HibernateException {
		if ( rowNumber >= 0 ) {
			current = first + rowNumber;
		}
		else {
			current = max + rowNumber + 1; //max row start at -1
		}
		return current >= first && current <= max;
	}
}
