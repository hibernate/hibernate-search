/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.stat.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.engine.Version;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.stat.Statistics;
import org.hibernate.search.stat.spi.StatisticsImplementor;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * A concurrent implementation of the {@code Statistics} interface.
 *
 * @author Hardy Ferentschik
 */
public class StatisticsImpl implements Statistics, StatisticsImplementor {

	private AtomicLong searchQueryCount = new AtomicLong();
	private AtomicLong searchExecutionTotalTime = new AtomicLong();
	private AtomicLong searchExecutionMaxTime = new AtomicLong();
	private volatile String queryExecutionMaxTimeQueryString;

	private AtomicLong objectLoadedCount = new AtomicLong();
	private AtomicLong objectLoadTotalTime = new AtomicLong();
	private AtomicLong objectLoadMaxTime = new AtomicLong();

	private volatile boolean isStatisticsEnabled;

	private final Lock readLock;
	private final Lock writeLock;

	private final ExtendedSearchIntegrator extendedIntegrator;

	public StatisticsImpl(ExtendedSearchIntegrator extendedIntegrator) {
		ReadWriteLock lock = new ReentrantReadWriteLock();
		readLock = lock.readLock();
		writeLock = lock.writeLock();

		this.extendedIntegrator = extendedIntegrator;
	}

	@Override
	public void clear() {
		searchQueryCount.set( 0 );
		searchExecutionTotalTime.set( 0 );
		searchExecutionMaxTime.set( 0 );
		queryExecutionMaxTimeQueryString = "";

		objectLoadedCount.set( 0 );
		objectLoadMaxTime.set( 0 );
		objectLoadTotalTime.set( 0 );
	}

	@Override
	public long getSearchQueryExecutionCount() {
		return searchQueryCount.get();
	}

	@Override
	public long getSearchQueryTotalTime() {
		return searchExecutionTotalTime.get();
	}

	@Override
	public long getSearchQueryExecutionMaxTime() {
		return searchExecutionMaxTime.get();
	}

	@Override
	public long getSearchQueryExecutionAvgTime() {
		writeLock.lock();
		try {
			long avgExecutionTime = 0;
			if ( searchQueryCount.get() > 0 ) {
				avgExecutionTime = searchExecutionTotalTime.get() / searchQueryCount.get();
			}
			return avgExecutionTime;
		}
		finally {
			writeLock.unlock();
		}
	}

	@Override
	public String getSearchQueryExecutionMaxTimeQueryString() {
		return queryExecutionMaxTimeQueryString;
	}

	@Override
	public void searchExecuted(String searchString, long time) {
		readLock.lock();
		try {
			boolean isLongestQuery = false;
			for ( long old = searchExecutionMaxTime.get();
					( time > old ) && ( isLongestQuery = searchExecutionMaxTime.compareAndSet( old, time ) );
					old = searchExecutionMaxTime.get() ) {
				// no-op
			}
			if ( isLongestQuery ) {
				queryExecutionMaxTimeQueryString = searchString;
			}
			searchQueryCount.getAndIncrement();
			searchExecutionTotalTime.addAndGet( time );
		}
		finally {
			readLock.unlock();
		}
	}

	@Override
	public long getObjectsLoadedCount() {
		return objectLoadedCount.get();
	}

	@Override
	public long getObjectLoadingTotalTime() {
		return objectLoadTotalTime.get();
	}

	@Override
	public long getObjectLoadingExecutionMaxTime() {
		return objectLoadMaxTime.get();
	}

	@Override
	public long getObjectLoadingExecutionAvgTime() {
		writeLock.lock();
		try {
			long avgLoadingTime = 0;
			if ( objectLoadedCount.get() > 0 ) {
				avgLoadingTime = objectLoadTotalTime.get() / objectLoadedCount.get();
			}
			return avgLoadingTime;
		}
		finally {
			writeLock.unlock();
		}
	}

	@Override
	public void objectLoadExecuted(long numberOfObjectsLoaded, long time) {
		readLock.lock();
		try {
			for ( long old = objectLoadMaxTime.get();
				( time > old ) && ( objectLoadMaxTime.compareAndSet( old, time ) );
				old = objectLoadMaxTime.get() ) {
				//no-op
			}
			objectLoadedCount.addAndGet( numberOfObjectsLoaded );
			objectLoadTotalTime.addAndGet( time );
		}
		finally {
			readLock.unlock();
		}
	}

	@Override
	public boolean isStatisticsEnabled() {
		return isStatisticsEnabled;
	}

	@Override
	public void setStatisticsEnabled(boolean b) {
		isStatisticsEnabled = b;
	}

	@Override
	public String getSearchVersion() {
		return Version.getVersionString();
	}

	@Override
	public Set<String> getIndexedClassNames() {
		Set<String> indexedClasses = new HashSet<String>();
		for ( Class clazz : extendedIntegrator.getIndexBindings().keySet() ) {
			indexedClasses.add( clazz.getName() );
		}
		return indexedClasses;
	}

	@Override
	public int getNumberOfIndexedEntities(String entity) {
		Class<?> clazz = getEntityClass( entity );
		IndexReader indexReader = extendedIntegrator.getIndexReaderAccessor().open( clazz );
		try {
			IndexSearcher searcher = new IndexSearcher( indexReader );
			BooleanQuery boolQuery = new BooleanQuery();
			boolQuery.add( new MatchAllDocsQuery(), BooleanClause.Occur.FILTER );
			boolQuery.add(
					new TermQuery( new Term( ProjectionConstants.OBJECT_CLASS, entity ) ), BooleanClause.Occur.FILTER
			);
			try {
				TopDocs topdocs = searcher.search( boolQuery, 1 );
				return topdocs.totalHits;
			}
			catch (IOException e) {
				throw new SearchException( "Unable to execute count query for entity " + entity, e );
			}
		}
		finally {
			extendedIntegrator.getIndexReaderAccessor().close( indexReader );
		}
	}

	@Override
	public Map<String, Integer> indexedEntitiesCount() {
		Map<String, Integer> countPerEntity = new HashMap<String, Integer>();
		for ( String className : getIndexedClassNames() ) {
			countPerEntity.put( className, getNumberOfIndexedEntities( className ) );
		}
		return countPerEntity;
	}

	private Class<?> getEntityClass(String entity) {
		Class<?> clazz;
		try {
			clazz = ClassLoaderHelper.classForName( entity, extendedIntegrator.getServiceManager() );
		}
		catch (ClassLoadingException e) {
			throw new IllegalArgumentException( entity + "not a indexed entity" );
		}
		return clazz;
	}
}


