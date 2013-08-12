/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.SearchException;
import org.hibernate.search.Version;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
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

	private final SearchFactoryImplementor searchFactoryImplementor;

	public StatisticsImpl(SearchFactoryImplementor searchFactoryImplementor) {
		ReadWriteLock lock = new ReentrantReadWriteLock();
		readLock = lock.readLock();
		writeLock = lock.writeLock();

		this.searchFactoryImplementor = searchFactoryImplementor;
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
		for ( Class clazz : searchFactoryImplementor.getIndexBindings().keySet() ) {
			indexedClasses.add( clazz.getName() );
		}
		return indexedClasses;
	}

	@Override
	public int getNumberOfIndexedEntities(String entity) {
		Class<?> clazz = getEntityClass( entity );
		IndexReader indexReader = searchFactoryImplementor.getIndexReaderAccessor().open( clazz );
		try {
			IndexSearcher searcher = new IndexSearcher( indexReader );
			BooleanQuery boolQuery = new BooleanQuery();
			boolQuery.add( new MatchAllDocsQuery(), BooleanClause.Occur.MUST );
			boolQuery.add(
					new TermQuery( new Term( ProjectionConstants.OBJECT_CLASS, entity ) ), BooleanClause.Occur.MUST
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
			searchFactoryImplementor.getIndexReaderAccessor().close( indexReader );
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
			clazz = ClassLoaderHelper.classForName( entity, StatisticsImpl.class.getClassLoader() );
		}
		catch (ClassNotFoundException e) {
			throw new IllegalArgumentException( entity + "not a indexed entity" );
		}
		return clazz;
	}
}


