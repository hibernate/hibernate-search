/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.query.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.genericjpa.dto.impl.DtoQueryExecutor;
import org.hibernate.search.genericjpa.entity.EntityProvider;
import org.hibernate.search.genericjpa.query.HSearchQuery;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.spi.SearchIntegrator;

public class HSearchQueryImpl implements HSearchQuery {

	private static final Logger LOGGER = Logger.getLogger( HSearchQueryImpl.class.getName() );

	private final HSQuery hsquery;
	private final DtoQueryExecutor queryExec;
	private final SearchIntegrator searchIntegrator;

	private Map<String, Object> hints = Collections.emptyMap();

	public HSearchQueryImpl(HSQuery hsquery, DtoQueryExecutor queryExec, SearchIntegrator searchIntegrator) {
		this.hsquery = hsquery;
		this.queryExec = queryExec;
		this.searchIntegrator = searchIntegrator;
	}

	@Override
	public HSearchQuery sort(Sort sort) {
		this.hsquery.sort( sort );
		return this;
	}

	@Override
	public HSearchQuery filter(Filter filter) {
		this.hsquery.filter( filter );
		return this;
	}

	@Override
	public HSearchQuery firstResult(int firstResult) {
		this.hsquery.firstResult( firstResult );
		return this;
	}

	@Override
	public HSearchQuery maxResults(int maxResults) {
		this.hsquery.maxResults( maxResults );
		return this;
	}

	@Override
	public Query getLuceneQuery() {
		return this.hsquery.getLuceneQuery();
	}

	@Override
	public <R> List<R> queryDto(Class<R> returnedType) {
		return this.queryExec.executeHSQuery( this.hsquery, returnedType );
	}

	@Override
	public <R> List<R> queryDto(Class<R> returnedType, String profileName) {
		return this.queryExec.executeHSQuery( this.hsquery, returnedType, profileName );
	}

	@Override
	public List<Object[]> queryProjection(String... projection) {
		String[] projectedFieldsBefore = this.hsquery.getProjectedFields();
		List<Object[]> ret;
		{
			this.hsquery.getTimeoutManager().start();

			this.hsquery.projection( projection );
			ret = this.hsquery.queryEntityInfos().stream().map(
					(entityInfo) -> entityInfo.getProjection()
			).collect( Collectors.toList() );

			this.hsquery.getTimeoutManager().stop();
		}
		this.hsquery.projection( projectedFieldsBefore );
		return ret;
	}

	@Override
	public int queryResultSize() {
		this.hsquery.getTimeoutManager().start();
		int resultSize = this.hsquery.queryResultSize();
		this.hsquery.getTimeoutManager().stop();
		return resultSize;
	}

	@Override
	public FullTextFilter enableFullTextFilter(String name) {
		return hsquery.enableFullTextFilter( name );
	}

	@Override
	public void disableFullTextFilter(String name) {
		this.hsquery.disableFullTextFilter( name );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public List query(EntityProvider entityProvider, Fetch fetchType) {
		List<Object> ret;
		List<Object[]> projected = this.queryProjection( ProjectionConstants.OBJECT_CLASS, ProjectionConstants.ID );
		if ( fetchType == Fetch.FIND_BY_ID ) {
			ret = projected.stream().map(
					(arr) -> {
						if ( arr[1] == null ) {
							LOGGER.info( "null id in index ommited for query" );
							return null;
						}
						Object obj = entityProvider.get( (Class<?>) arr[0], arr[1], this.hints );
						if ( obj == null ) {
							LOGGER.info( "ommiting object of class " + arr[0] + " and id " + arr[1] + " which was found in the index but not in the database!" );
						}
						return obj;
					}
			).filter( (obj) -> obj != null ).collect( Collectors.toList() );
		}
		else {
			ret = new ArrayList<>( projected.size() );
			Map<Class<?>, List<Object>> idsForClass = new HashMap<>();
			List<Object[]> originalOrder = new ArrayList<>();
			Map<Class<?>, Map<Object, Object>> classToIdToObject = new HashMap<>();
			// split the ids for each class (and also make sure the original
			// order is saved. this is needed even for only one class)
			projected.stream().forEach(
					(arr) -> {
						if ( arr[1] == null ) {
							LOGGER.info( "null id in index ommited for query" );
							return;
						}
						originalOrder.add( arr );
						idsForClass.computeIfAbsent(
								(Class<?>) arr[0], (clazz) -> new ArrayList<>()
						).add( arr[1] );
						// just make sure the map is already created,
						// we do this here to not clutter the following code
						classToIdToObject.computeIfAbsent( (Class<?>) arr[0], (clz) -> new HashMap<>() );
					}
			);
			// get all entities of the same type in one batch
			idsForClass.entrySet().forEach(
					(entry) ->
							entityProvider.getBatch( entry.getKey(), entry.getValue(), this.hints ).stream().forEach(
									(object) -> {
										Class<?> entityClass = entry.getKey();
										Object id = this.searchIntegrator.getIndexBinding( entityClass )
												.getDocumentBuilder()
												.getId( object );
										classToIdToObject.get( entityClass ).put(
												id,
												object
										);
									}
							)
			);
			// and put everything back into order
			originalOrder.stream().forEach(
					(arr) -> {
						Object value = classToIdToObject.get( arr[0] ).get( arr[1] );
						if ( value == null ) {
							LOGGER.info( "ommiting object of class " + arr[0] + " and id " + arr[1] + " which was found in the index but not in the database!" );
						}
						else {
							ret.add( classToIdToObject.get( arr[0] ).get( arr[1] ) );
						}
					}
			);
		}
		if ( ret.size() != projected.size() ) {
			LOGGER.info( "returned size was not equal to projected size" );
		}
		return ret;
	}

	@Override
	public HSearchQuery setTimeout(long timeout, TimeUnit timeUnit) {
		this.hsquery.getTimeoutManager().setTimeout( timeout, timeUnit );
		this.hsquery.getTimeoutManager().raiseExceptionOnTimeout();
		return this;
	}

	@Override
	public HSearchQuery limitExecutionTimeTo(long timeout, TimeUnit timeUnit) {
		this.hsquery.getTimeoutManager().setTimeout( timeout, timeUnit );
		this.hsquery.getTimeoutManager().limitFetchingOnTimeout();
		return this;
	}

	@Override
	public boolean hasPartialResults() {
		return this.hsquery.getTimeoutManager().hasPartialResults();
	}

	@Override
	public Explanation explain(int documentId) {
		return this.hsquery.explain( documentId );
	}

	@Override
	public String toString() {
		return this.hsquery.getLuceneQuery().toString();
	}

	@Override
	public FacetManager getFacetManager() {
		return this.hsquery.getFacetManager();
	}

	@Override
	public HSearchQuery setSpatialParameters(double latitude, double longitude, String fieldName) {
		this.setSpatialParameters( Point.fromDegrees( latitude, longitude ), fieldName );
		return this;
	}

	@Override
	public HSearchQuery setSpatialParameters(Coordinates center, String fieldName) {
		this.hsquery.setSpatialParameters( center, fieldName );
		return this;
	}

	@Override
	public HSearchQuery hints(Map<String, Object> hints) {
		if ( hints == null ) {
			this.hints = Collections.emptyMap();
		}
		else {
			this.hints = Collections.unmodifiableMap( hints );
		}
		return this;
	}

}
