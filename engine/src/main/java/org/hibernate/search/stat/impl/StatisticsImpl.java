/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.stat.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.IndexedTypeIdentifier;
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
 * @author Sanne Grinovero
 */
public class StatisticsImpl implements Statistics, StatisticsImplementor {

	// The following four fields need always be updated
	// as a group. We use the exclusiveLock only on reads though!
	// The hot path is on writing, while a read might never happen
	// and we only care about consistent reads, since races during
	// writes won't loose any data.
	private LongAdder searchQueryCount = new LongAdder();
	private LongAdder searchExecutionTotalTime = new LongAdder();
	// Following is not a LongAdder as it's frequently read while producing stats
	private AtomicLong searchExecutionMaxTime = new AtomicLong();
	private volatile String queryExecutionMaxTimeQueryString;

	private LongAdder objectLoadedCount = new LongAdder();
	private LongAdder objectLoadTotalTime = new LongAdder();
	// Following is not a LongAdder as it's frequently read while producing stats
	private AtomicLong objectLoadMaxTime = new AtomicLong();

	private volatile boolean isStatisticsEnabled;

	private final Lock sharedLock;
	private final Lock exclusiveLock;

	private final ExtendedSearchIntegrator extendedIntegrator;

	public StatisticsImpl(ExtendedSearchIntegrator extendedIntegrator) {
		ReadWriteLock lock = new ReentrantReadWriteLock();
		sharedLock = lock.readLock();
		exclusiveLock = lock.writeLock();
		this.extendedIntegrator = extendedIntegrator;
	}

	@Override
	public void clear() {
		searchQueryCount.reset();
		searchExecutionTotalTime.reset();
		searchExecutionMaxTime.set( 0 );
		queryExecutionMaxTimeQueryString = "";
		objectLoadedCount.reset();
		objectLoadMaxTime.set( 0 );
		objectLoadTotalTime.reset();
	}

	@Override
	public long getSearchQueryExecutionCount() {
		return searchQueryCount.longValue();
	}

	@Override
	public long getSearchQueryTotalTime() {
		return searchExecutionTotalTime.longValue();
	}

	@Override
	public long getSearchQueryExecutionMaxTime() {
		return searchExecutionMaxTime.longValue();
	}

	@Override
	public long getSearchQueryExecutionAvgTime() {
		final long searchQueryCountLocal;
		final long searchExecutionTotalTimeLocal;
		exclusiveLock.lock();
		try {
			searchQueryCountLocal = searchQueryCount.longValue();
			searchExecutionTotalTimeLocal = searchExecutionTotalTime.longValue();
		}
		finally {
			exclusiveLock.unlock();
		}
		long avgExecutionTime = 0;
		if ( searchQueryCountLocal > 0 ) {
			avgExecutionTime = searchExecutionTotalTimeLocal / searchQueryCountLocal;
		}
		return avgExecutionTime;
	}

	@Override
	public String getSearchQueryExecutionMaxTimeQueryString() {
		return queryExecutionMaxTimeQueryString;
	}

	@Override
	public void searchExecuted(String searchString, long time) {
		sharedLock.lock();
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
			searchQueryCount.increment();
			searchExecutionTotalTime.add( time );
		}
		finally {
			sharedLock.unlock();
		}
	}

	@Override
	public long getObjectsLoadedCount() {
		return objectLoadedCount.longValue();
	}

	@Override
	public long getObjectLoadingTotalTime() {
		return objectLoadTotalTime.longValue();
	}

	@Override
	public long getObjectLoadingExecutionMaxTime() {
		return objectLoadMaxTime.longValue();
	}

	@Override
	public long getObjectLoadingExecutionAvgTime() {
		exclusiveLock.lock();
		try {
			long avgLoadingTime = 0;
			final long currentObjectLoadedCount = objectLoadedCount.longValue();
			if ( currentObjectLoadedCount > 0 ) {
				avgLoadingTime = objectLoadTotalTime.longValue() / currentObjectLoadedCount;
			}
			return avgLoadingTime;
		}
		finally {
			exclusiveLock.unlock();
		}
	}

	@Override
	public void objectLoadExecuted(long numberOfObjectsLoaded, long time) {
		sharedLock.lock();
		try {
			for ( long old = objectLoadMaxTime.longValue();
				( time > old ) && ( objectLoadMaxTime.compareAndSet( old, time ) );
				old = objectLoadMaxTime.longValue() ) {
				//no-op
			}
			objectLoadedCount.add( numberOfObjectsLoaded );
			objectLoadTotalTime.add( time );
		}
		finally {
			sharedLock.unlock();
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
		for ( IndexedTypeIdentifier clazz : extendedIntegrator.getIndexBindings().keySet() ) {
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
			BooleanQuery boolQuery = new BooleanQuery.Builder()
					.add( new MatchAllDocsQuery(), BooleanClause.Occur.FILTER )
					.add( new TermQuery( new Term( ProjectionConstants.OBJECT_CLASS, entity ) ),
							BooleanClause.Occur.FILTER )
					.build();
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

	@Override
	public long getIndexSize(String indexName) {
		IndexManager indexManager = extendedIntegrator.getIndexManager( indexName );
		if ( indexManager == null ) {
			throw new IllegalArgumentException( "'" + indexName + "' is not a known index" );
		}
		return getIndexSize( indexManager );
	}

	@Override
	public Map<String, Long> indexSizes() {
		return extendedIntegrator.getIndexManagerHolder().getIndexManagers().stream()
				.collect( Collectors.toMap( IndexManager::getIndexName, this::getIndexSize ) );
	}

	private long getIndexSize(IndexManager indexManager) {
		if ( !( indexManager instanceof DirectoryBasedIndexManager ) ) {
			throw new IllegalArgumentException( "Index '" + indexManager.getIndexName()
					+ "' is not a Lucene index" );
		}

		DirectoryBasedIndexManager directoryBasedIndexManager = (DirectoryBasedIndexManager) indexManager;
		Directory directory = directoryBasedIndexManager.getDirectoryProvider().getDirectory();

		long totalSize = 0l;
		try {
			for ( String fileName : directory.listAll() ) {
				try {
					totalSize += directory.fileLength( fileName );
				}
				catch (FileNotFoundException ignored) {
					// Ignore: the file was probably removed since the call to listAll
				}
			}
		}
		catch (IOException e) {
			throw new SearchException( "Unexpected exception while computing size of index '"
					+ indexManager.getIndexName() + "'", e );
		}

		return totalSize;
	}
}


