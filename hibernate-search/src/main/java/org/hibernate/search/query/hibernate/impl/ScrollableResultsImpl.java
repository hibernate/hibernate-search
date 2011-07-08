/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.query.hibernate.impl;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.SearchException;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.type.Type;

/**
 * Implements scrollable and paginated resultsets.
 * Contrary to Query#iterate() or Query#list(), this implementation is
 * exposed to returned null objects (if the index is out of date).
 * <p/>
 * <p/>
 * The following methods that change the value of 'current' will check
 * and set its value to either 'afterLast' or 'beforeFirst' depending
 * on direction. This is to prevent rogue values from setting it outside
 * the boundaries of the results.
 * <ul>
 * <li>next()</li>
 * <li>previous()</li>
 * <li>scroll(i)</li>
 * <li>last()</li>
 * <li>first()</li>
 * </ul>
 * 
 * @see org.hibernate.Query
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Sanne Grinovero
 */
public class ScrollableResultsImpl implements ScrollableResults {
	
	private static final Log log = LoggerFactory.make();

	private final int first;
	private final int max;
	private final int fetchSize;
	private final Loader loader;
	private final DocumentExtractor documentExtractor;
	private final SessionImplementor session;
	
	/**
	 * Caches result rows and EntityInfo from
	 * <code>first</code> to <code>max</code>
	 */
	private final LoadedObject[] resultsContext;
	
	private int current;

	public ScrollableResultsImpl(int fetchSize, DocumentExtractor extractor,
			Loader loader, SessionImplementor sessionImplementor
	) {
		this.loader = loader;
		this.documentExtractor = extractor;
		this.fetchSize = fetchSize;
		this.session = sessionImplementor;
		this.first = extractor.getFirstIndex();
		this.max = extractor.getMaxIndex();
		int size = Math.max( max - first + 1, 0 );
		this.resultsContext = new LoadedObject[size];
		beforeFirst();
	}

	private LoadedObject ensureCurrentLoaded() {
		LoadedObject currentCacheRef = resultsContext[current - first];
		if ( currentCacheRef != null ) {
			return currentCacheRef;
		}
		// the loading window is optimized for scrolling in both directions:
		int windowStop = Math.min( max + 1 , current + fetchSize );
		int windowStart = Math.max( first, current - fetchSize + 1 );
		List<EntityInfo> entityInfosToLoad = new ArrayList<EntityInfo>( fetchSize );
		int sizeToLoad = 0;
		for (int x = windowStart; x < windowStop; x++) {
			int arrayIdx = x - first;
			LoadedObject lo = resultsContext[arrayIdx];
			if ( lo == null ) {
				lo = new LoadedObject();
				// makes hard references and extract EntityInfos:
				entityInfosToLoad.add( lo.getEntityInfo( x ) );
				resultsContext[arrayIdx] = lo;
				sizeToLoad++;
				if ( sizeToLoad >= fetchSize )
					break;
			}
		}
		//preload efficiently by batches:
		if ( sizeToLoad > 1 ) {
			loader.load( entityInfosToLoad.toArray( new EntityInfo[sizeToLoad] ) );
			//(no references stored at this point: they still need to be loaded one by one to inject null results)
		}
		return resultsContext[ current - first ];
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean next() {
		//	Increases cursor pointer by one. If this places it >
		//	max + 1 (afterLast) then set it to afterLast and return
		//	false.
		if ( ++current > max ) {
			afterLast();
			return false;
		}
		return true;
	}

	public boolean previous() {
		//	Decreases cursor pointer by one. If this places it <
		//	first - 1 (beforeFirst) then set it to beforeFirst and
		//	return false.
		if ( --current < first ) {
			beforeFirst();
			return false;
		}
		return true;
	}

	public boolean scroll(int i) {
		//  Since we have to take into account that we can scroll any
		//  amount positive or negative, we perform the same tests that
		//  we performed in next() and previous().
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

	public boolean last() {
		current = max;
		if ( current < first ) {
			beforeFirst();
			return false;
		}
		return max >= first;
	}

	public boolean first() {
		current = first;
		if ( current > max ) {
			afterLast();
			return false;
		}
		return max >= first;
	}

	public void beforeFirst() {
		current = first - 1;
	}

	public void afterLast() {
		current = max + 1;
		//TODO help gc by clearing all structures when using forwardonly scrollmode.
	}

	public boolean isFirst() {
		return current == first;
	}

	public boolean isLast() {
		return current == max;
	}

	public void close() {
		try {
			documentExtractor.close();
		}
		catch (SearchException e) {
			log.unableToCloseSearcherInScrollableResult( e );
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
		LoadedObject cacheEntry = ensureCurrentLoaded();
		return cacheEntry.getManagedResult( current );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Object get(int i) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Type getType(int i) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Integer getInteger(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Long getLong(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Float getFloat(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Boolean getBoolean(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Double getDouble(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Short getShort(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Byte getByte(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Character getCharacter(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public byte[] getBinary(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public String getText(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Blob getBlob(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Clob getClob(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public String getString(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public BigDecimal getBigDecimal(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public BigInteger getBigInteger(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Date getDate(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Locale getLocale(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public Calendar getCalendar(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	/**
	 * This method is not supported on Lucene based queries
	 * @throws UnsupportedOperationException always thrown
	 */
	public TimeZone getTimeZone(int col) {
		throw new UnsupportedOperationException( "Lucene does not work on columns" );
	}

	public int getRowNumber() {
		if ( max < first ) return -1;
		return current - first;
	}

	public boolean setRowNumber(int rowNumber) {
		if ( rowNumber >= 0 ) {
			current = first + rowNumber;
		}
		else {
			current = max + rowNumber + 1; //max row start at -1
		}
		return current >= first && current <= max;
	}
	
	private final class LoadedObject {
		
		private Reference<Object[]> entity; //never==null but Reference.get can return null
		private Reference<EntityInfo> einfo; //never==null but Reference.get can return null
		
		/**
		 * Gets the objects from cache if it is available and attached to session,
		 * or reload them and update the cache entry.
		 * @param x absolute position in fulltext result.
		 * @return the managed objects
		 */
		private Object[] getManagedResult(int x) {
			EntityInfo entityInfo = getEntityInfo( x );
			Object[] objects = entity==null ? null : entity.get();
			if ( objects!=null && areAllEntitiesManaged( objects, entityInfo ) ) {
				return objects;
			}
			else {
				Object loaded = loader.load( entityInfo );
				if ( ! loaded.getClass().isArray() ) loaded = new Object[] { loaded };
				objects = (Object[]) loaded;
				this.entity = new SoftReference<Object[]>( objects );
				return objects;
			}
		}

		/**
		 * Extract an entityInfo, either from cache or from the index.
		 * @param x the position in the index.
		 * @return
		 */
		private EntityInfo getEntityInfo(int x) {
			EntityInfo entityInfo = einfo==null ? null : einfo.get();
			if ( entityInfo==null ) {
				try {
					entityInfo = documentExtractor.extract( x );
				}
				catch (IOException e) {
					throw new SearchException( "Unable to read Lucene topDocs[" + x + "]", e );
				}
				einfo = new SoftReference<EntityInfo>( entityInfo );
			}
			return entityInfo;
		}

	}
	
	private boolean areAllEntitiesManaged(Object[] objects,	EntityInfo entityInfo) {
		//check if all entities are session-managed and skip the check on projected values
		org.hibernate.Session hibSession = (org.hibernate.Session) session;
		if ( entityInfo.getProjection() != null ) {
			// using projection: test only for entities
			for ( int idx : entityInfo.getIndexesOfThis() ) {
				Object o = objects[idx];
				//TODO improve: is it useful to check for proxies and have them reassociated to persistence context?
				if ( ! hibSession.contains( o ) )
					return false;
			}
			return true;
		}
		else {
			return hibSession.contains( objects[0] );
		}
	}
	
}
