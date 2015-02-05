/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;

import org.hibernate.Session;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.manualsource.FullTextQuery;
import org.hibernate.search.manualsource.WorkLoad;
import org.hibernate.search.manualsource.source.ObjectInitializer;
import org.hibernate.search.query.engine.QueryTimeoutException;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class FullTextQueryImpl implements FullTextQuery {

	private static final Log log = LoggerFactory.make();
	private final WorkLoadImpl workLoadImpl;

	private final HSQuery hSearchQuery;

	/**
	 * Constructs a  <code>FullTextQueryImpl</code> instance.
	 */
	public FullTextQueryImpl(org.apache.lucene.search.Query query,
							 Class<?>[] classes,
							 WorkLoadImpl workLoad) {
		workLoadImpl = workLoad;

		ExtendedSearchIntegrator extendedIntegrator = workLoad.getWorkLoadManager().getSearchIntegrator();

		hSearchQuery = extendedIntegrator.createHSQuery();
		hSearchQuery
				.luceneQuery( query )
				.timeoutExceptionFactory( exceptionFactory )
				.targetedEntities( Arrays.asList( classes ) );
	}

	@Override
	public FullTextQuery setSort(Sort sort) {
		hSearchQuery.sort( sort );
		return this;
	}

	@Override
	public FullTextQuery setFilter(Filter filter) {
		hSearchQuery.filter( filter );
		return this;
	}

	/**
	 * Return an iterator on the results.
	 * Retrieve the object one by one (initialize it during the next() operation)
	 */
	@Override
	public Iterator iterate() {
		//implement an iterator which keep the id/class for each hit and get the object on demand
		//cause I can't keep the searcher and hence the hit opened. I don't have any hook to know when the
		//user stops using it
		//scrollable is better in this area

		hSearchQuery.getTimeoutManager().start();
		final List<EntityInfo> entityInfos = hSearchQuery.queryEntityInfos();
		//stop timeout manager, the iterator pace is in the user's hands
		hSearchQuery.getTimeoutManager().stop();
		//TODO is this no-loader optimization really needed?
		final Iterator<Object> iterator;
		if ( entityInfos.size() == 0 ) {
			iterator = new IteratorImpl( entityInfos, noLoader );
			return iterator;
		}
		else {
			Loader loader = getLoader();
			iterator = new IteratorImpl( entityInfos, loader );
		}
		hSearchQuery.getTimeoutManager().stop();
		return iterator;
	}


	/**
	 * Decide which object loader to use depending on the targeted entities. If there is only a single entity targeted
	 * a <code>QueryLoader</code> can be used which will only execute a single query to load the entities. If more than
	 * one entity is targeted a <code>MultiClassesQueryLoader</code> must be used. We also have to consider whether
	 * projections or <code>Criteria</code> are used.
	 *
	 * @return The loader instance to use to load the results of the query.
	 */
	private Loader getLoader() {
		ObjectLoaderBuilder loaderBuilder = new ObjectLoaderBuilder()
				.targetedEntities( hSearchQuery.getTargetedEntities() )
				.indexedTargetedEntities( hSearchQuery.getIndexedTargetedEntities() )
				.workLoad( workLoadImpl )
				.searchFactory( hSearchQuery.getExtendedSearchIntegrator() )
				.timeoutManager( hSearchQuery.getTimeoutManager() );
		if ( hSearchQuery.getProjectedFields() != null ) {
			return getProjectionLoader( loaderBuilder );
		}
		else {
			return loaderBuilder.buildLoader();
		}
	}

	private Loader getProjectionLoader(ObjectLoaderBuilder loaderBuilder) {
		ProjectionLoader loader = new ProjectionLoader();
		loader.init(
				workLoadImpl,
				hSearchQuery.getExtendedSearchIntegrator(),
				loaderBuilder,
				hSearchQuery.getProjectedFields(),
				hSearchQuery.getTimeoutManager()
		);
		return loader;
	}

	@Override
	public List list() {
		hSearchQuery.getTimeoutManager().start();
		final List<EntityInfo> entityInfos = hSearchQuery.queryEntityInfos();
		Loader loader = getLoader();
		List list = loader.load( entityInfos.toArray( new EntityInfo[entityInfos.size()] ) );
		//no need to timeoutManager.isTimedOut from this point, we don't do anything intensive
		hSearchQuery.getTimeoutManager().stop();
		return list;
	}

	@Override
	public Explanation explain(int documentId) {
		return hSearchQuery.explain( documentId );
	}

	@Override
	public int getResultSize() {
		if ( getLoader().isSizeSafe() ) {
			return hSearchQuery.queryResultSize();
		}
		else {
			throw new AssertionFailure( "All current implementations of Loader should be SizeSafe I think" );
		}
	}

	@Override
	public FullTextQuery setProjection(String... fields) {
		hSearchQuery.projection( fields );
		return this;
	}

	@Override
	public FullTextQuery setSpatialParameters(Coordinates center, String fieldName) {
		hSearchQuery.setSpatialParameters( center, fieldName );
		return this;
	}

	@Override
	public FullTextQuery setSpatialParameters(double latitude, double longitude, String fieldName) {
		setSpatialParameters( Point.fromDegrees( latitude, longitude ), fieldName );
		return this;
	}

	@Override
	public FullTextQuery setFirstResult(int firstResult) {
		hSearchQuery.firstResult( firstResult );
		return this;
	}

	@Override
	public FullTextQuery setMaxResults(int maxResults) {
		hSearchQuery.maxResults( maxResults );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> type) {
		if ( type == org.apache.lucene.search.Query.class ) {
			return (T) hSearchQuery.getLuceneQuery();
		}
		throw new IllegalArgumentException( "Cannot unwrap " + type.getName() );
	}

	@Override
	public FullTextFilter enableFullTextFilter(String name) {
		return hSearchQuery.enableFullTextFilter( name );
	}

	@Override
	public void disableFullTextFilter(String name) {
		hSearchQuery.disableFullTextFilter( name );
	}

	@Override
	public FacetManager getFacetManager() {
		return hSearchQuery.getFacetManager();
	}

	@Override
	public FullTextQuery setTimeout(long timeout, TimeUnit timeUnit) {
		hSearchQuery.getTimeoutManager().setTimeout( timeout, timeUnit );
		hSearchQuery.getTimeoutManager().raiseExceptionOnTimeout();
		return this;
	}

	@Override
	public FullTextQuery limitExecutionTimeTo(long timeout, TimeUnit timeUnit) {
		hSearchQuery.getTimeoutManager().setTimeout( timeout, timeUnit );
		hSearchQuery.getTimeoutManager().limitFetchingOnTimeout();
		return this;
	}

	@Override
	public boolean hasPartialResults() {
		return hSearchQuery.getTimeoutManager().hasPartialResults();
	}

	private static final Loader noLoader = new Loader() {
		@Override
		public void init(WorkLoadImpl workLoad,
						 ExtendedSearchIntegrator extendedIntegrator,
						 ObjectInitializer objectInitializer,
						 TimeoutManager timeoutManager) {
		}

		@Override
		public Object load(EntityInfo entityInfo) {
			throw new UnsupportedOperationException( "noLoader should not be used" );
		}

		@Override
		public Object loadWithoutTiming(EntityInfo entityInfo) {
			throw new UnsupportedOperationException( "noLoader should not be used" );
		}

		@Override
		public List load(EntityInfo... entityInfos) {
			throw new UnsupportedOperationException( "noLoader should not be used" );
		}

		@Override
		public boolean isSizeSafe() {
			return false;
		}
	};

	private static final TimeoutExceptionFactory exceptionFactory = QueryTimeoutException.DEFAULT_TIMEOUT_EXCEPTION_FACTORY;
}
