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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.QueryTimeoutException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.internal.AbstractQueryImpl;
import org.hibernate.search.FullTextFilter;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.util.impl.ContextHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.transform.ResultTransformer;

/**
 * Implementation of {@link org.hibernate.search.FullTextQuery}.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Hardy Ferentschik <hardy@hibernate.org>
 */
public class FullTextQueryImpl extends AbstractQueryImpl implements FullTextQuery {

	private static final Log log = LoggerFactory.make();
	private Criteria criteria;
	private ResultTransformer resultTransformer;
	private int fetchSize = 1;
	private ObjectLookupMethod lookupMethod = ObjectLookupMethod.SKIP; //default
	private DatabaseRetrievalMethod retrievalMethod = DatabaseRetrievalMethod.QUERY; //default
	private final HSQuery hSearchQuery;

	/**
	 * Constructs a  <code>FullTextQueryImpl</code> instance.
	 *
	 * @param query The Lucene query.
	 * @param classes Array of classes (must be immutable) used to filter the results to the given class types.
	 * @param session Access to the Hibernate session.
	 * @param parameterMetadata Additional query metadata.
	 */
	public FullTextQueryImpl(org.apache.lucene.search.Query query, Class<?>[] classes, SessionImplementor session,
							ParameterMetadata parameterMetadata) {
		//TODO handle flushMode
		super( query.toString(), null, session, parameterMetadata );
		//TODO get a factory on searchFactoryImplementor
		hSearchQuery = getSearchFactoryImplementor().createHSQuery();
		hSearchQuery
				.luceneQuery( query )
				.timeoutExceptionFactory( exceptionFactory )
				.targetedEntities( Arrays.asList( classes ) );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FullTextQuery setSort(Sort sort) {
		hSearchQuery.sort( sort );
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
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
		//TODO is this noloader optimization really needed?
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
				.criteria( criteria )
				.targetedEntities( hSearchQuery.getTargetedEntities() )
				.indexedTargetedEntities( hSearchQuery.getIndexedTargetedEntities() )
				.session( session )
				.searchFactory( hSearchQuery.getSearchFactoryImplementor() )
				.timeoutManager( hSearchQuery.getTimeoutManager() )
				.lookupMethod( lookupMethod )
				.retrievalMethod( retrievalMethod );
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
				(Session) session,
				hSearchQuery.getSearchFactoryImplementor(),
				resultTransformer,
				loaderBuilder,
				hSearchQuery.getProjectedFields(),
				hSearchQuery.getTimeoutManager()
		);
		return loader;
	}

	@Override
	public ScrollableResults scroll() {
		//keep the searcher open until the resultset is closed

		hSearchQuery.getTimeoutManager().start();
		final DocumentExtractor documentExtractor = hSearchQuery.queryDocumentExtractor();
		//stop timeout manager, the iterator pace is in the user's hands
		hSearchQuery.getTimeoutManager().stop();
		Loader loader = getLoader();
		return new ScrollableResultsImpl(
				fetchSize,
				documentExtractor,
				loader,
				this.session
		);
	}

	@Override
	public ScrollableResults scroll(ScrollMode scrollMode) {
		//TODO think about this scrollmode
		return scroll();
	}

	@Override
	public List list() {
		hSearchQuery.getTimeoutManager().start();
		final List<EntityInfo> entityInfos = hSearchQuery.queryEntityInfos();
		Loader loader = getLoader();
		List list = loader.load( entityInfos.toArray( new EntityInfo[entityInfos.size()] ) );
		//no need to timeoutManager.isTimedOut from this point, we don't do anything intensive
		if ( resultTransformer == null || loader instanceof ProjectionLoader ) {
			//stay consistent with transformTuple which can only be executed during a projection
			//nothing to do
		}
		else {
			list = resultTransformer.transformList( list );
		}
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
			throw log.cannotGetResultSizeWithCriteriaAndRestriction( criteria.toString() );
		}
	}

	@Override
	public FullTextQuery setCriteriaQuery(Criteria criteria) {
		this.criteria = criteria;
		return this;
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
	public FullTextQuery setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		if ( fetchSize <= 0 ) {
			throw new IllegalArgumentException( "'fetch size' parameter less than or equals to 0" );
		}
		this.fetchSize = fetchSize;
		return this;
	}

	@Override
	public Query setLockOptions(LockOptions lockOptions) {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	@Override
	public FullTextQuery setResultTransformer(ResultTransformer transformer) {
		super.setResultTransformer( transformer );
		this.resultTransformer = transformer;
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
	public LockOptions getLockOptions() {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	@Override
	public int executeUpdate() {
		throw new UnsupportedOperationException( "executeUpdate is not supported in Hibernate Search queries" );
	}

	@Override
	public Query setLockMode(String alias, LockMode lockMode) {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
	}

	protected Map getLockModes() {
		throw new UnsupportedOperationException( "Lock options are not implemented in Hibernate Search queries" );
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
	public FullTextQuery setTimeout(int timeout) {
		return setTimeout( timeout, TimeUnit.SECONDS );
	}

	@Override
	public FullTextQuery setTimeout(long timeout, TimeUnit timeUnit) {
		super.setTimeout( (int) timeUnit.toSeconds( timeout ) );
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

	@Override
	public FullTextQuery initializeObjectsWith(ObjectLookupMethod lookupMethod, DatabaseRetrievalMethod retrievalMethod) {
		this.lookupMethod = lookupMethod;
		this.retrievalMethod = retrievalMethod;
		return this;
	}

	private SearchFactoryImplementor getSearchFactoryImplementor() {
		return ContextHelper.getSearchFactoryBySessionImplementor( session );
	}

	private static final Loader noLoader = new Loader() {
		@Override
		public void init(Session session,
						SearchFactoryImplementor searchFactoryImplementor,
						ObjectsInitializer objectsInitializer,
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

	private static final TimeoutExceptionFactory exceptionFactory = new TimeoutExceptionFactory() {

		@Override
		public RuntimeException createTimeoutException(String message, org.apache.lucene.search.Query luceneQuery) {
			return new QueryTimeoutException( message, (SQLException) null, luceneQuery.toString() );
		}

	};
}
