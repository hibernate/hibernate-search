/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.testsupport.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.V5MigrationSearchSession;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.testsupport.migration.V5MigrationStandalonePojoSearchSessionAdapter;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.common.AssertionFailure;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.ListAssert;

/**
 * A helper for Hibernate Search integration tests.
 * <p>
 * Provides easy-to-use primitives frequently used in integration tests.
 *
 * @author Yoann Rodiere
 */
public class SearchITHelper {

	private final SearchFactoryHolder sfHolder;

	public SearchITHelper(SearchFactoryHolder sfHolder) {
		this.sfHolder = sfHolder;
	}

	public WorkExecutor executor() {
		return new WorkExecutor();
	}

	public WorkExecutor executor(String tenantId) {
		return new WorkExecutor( tenantId );
	}

	public EntityInstanceWorkContext add() {
		return executor().add();
	}

	public void add(Iterable<?> entries) {
		add().push( entries ).execute();
	}

	public void add(Object... entries) {
		add().push( entries ).execute();
	}

	public void add(Object entry, Serializable id) {
		add().push( entry, id ).execute();
	}

	public EntityInstanceWorkContext index() {
		return executor().index();
	}

	public void index(Iterable<?> entries) {
		index().push( entries ).execute();
	}

	public void index(Object... entries) {
		index().push( entries ).execute();
	}

	public void index(Object entry, Serializable id) {
		index().push( entry, id ).execute();
	}

	public EntityTypeWorkContext delete() {
		return executor().delete();
	}

	public void delete(Class<?> type, Iterable<? extends Serializable> ids) {
		delete().push( type, ids ).execute();
	}

	public void delete(Class<?> type, Serializable... ids) {
		delete().push( type, ids ).execute();
	}

	public QueryBuilder queryBuilder(Class<?> clazz) {
		return sfHolder.getSearchFactory().buildQueryBuilder().forEntity( clazz ).get();
	}

	public V5MigrationSearchSession<?> session() {
		return new V5MigrationStandalonePojoSearchSessionAdapter( sfHolder.getMapping().createSession() );
	}

	public HSQuery hsQuery(Class<?>... classes) {
		return hsQuery( new MatchAllDocsQuery(), classes );
	}

	public HSQuery hsQuery(Query query, Class<?>... classes) {
		return sfHolder.getSearchFactory().createHSQuery( query, session(), null, classes );
	}

	public AssertBuildingHSQueryContext assertThatQuery(String fieldName, String value) {
		return assertThatQuery( termQuery( fieldName, value ) );
	}

	public AssertBuildingHSQueryContext assertThatQuery() {
		return assertThatQuery( new MatchAllDocsQuery() );
	}

	public AssertBuildingHSQueryContext assertThatQuery(Query luceneQuery) {
		return new AssertBuildingHSQueryContext( luceneQuery );
	}

	public AssertHSQueryContext assertThatQuery(HSQuery hsQuery) {
		return new AssertHSQueryContext() {
			@Override
			protected HSQuery getHSQuery() {
				return hsQuery;
			}
		};
	}

	public interface Identifiable<T extends Serializable> {
		T getId();
	}

	public class WorkExecutor {
		private final String tenantId;
		private List<Work> works = new ArrayList<>();

		public WorkExecutor() {
			this( null );
		}

		public WorkExecutor(String tenantId) {
			super();
			this.tenantId = tenantId;
		}

		public EntityInstanceWorkContext add() {
			return new EntityInstanceWorkContext( WorkType.ADD, this );
		}

		public EntityInstanceWorkContext index() {
			return new EntityInstanceWorkContext( WorkType.INDEX, this );
		}

		public EntityTypeWorkContext delete() {
			return new EntityTypeWorkContext( WorkType.DELETE, this );
		}

		public void push(Work work) {
			works.add( work );
		}

		public void push(Stream<? extends Work> works) {
			works.forEach( this.works::add );
		}

		public void execute() {
			try ( SearchSession session = sfHolder.getMapping().createSession() ) {
				SearchIndexer indexer = session.indexer();
				CompletableFuture<?>[] futures = new CompletableFuture[works.size()];
				for ( int i = 0; i < works.size(); i++ ) {
					futures[i] = executeWork( indexer, works.get( i ) ).toCompletableFuture();
				}
				CompletableFuture.allOf( futures ).join();
			}
			finally {
				works.clear();
			}
		}

		private CompletionStage<?> executeWork(SearchIndexer indexer, Work w) {
			switch ( w.workType ) {
				case ADD:
					return indexer.add( w.providedId, w.entity );
				case UPDATE:
				case INDEX:
					return indexer.addOrUpdate( w.providedId, w.entity );
				case DELETE:
					return indexer.delete( w.entityType, w.providedId, null );
				default:
					throw new AssertionFailure( "Unexpected work type: " + w.workType );
			}
		}
	}

	public class EntityInstanceWorkContext {
		private final WorkType workType;

		private WorkExecutor executor;

		private EntityInstanceWorkContext(WorkType workType, WorkExecutor executor) {
			super();
			this.workType = workType;
			this.executor = executor;
		}

		public EntityInstanceWorkContext push(Object entry, Serializable id) {
			executor.push( new Work( entry, id, workType ) );
			return this;
		}

		public EntityInstanceWorkContext push(Iterable<?> entries) {
			return push( StreamSupport.stream( entries.spliterator(), false ) );
		}

		public EntityInstanceWorkContext push(Object... entries) {
			return push( Arrays.stream( entries ) );
		}

		public EntityInstanceWorkContext push(Stream<?> entries) {
			executor.push( entries.map( e -> new Work( e, null, workType ) ) );
			return this;
		}

		public void execute() {
			executor.execute();
		}
	}

	private static class Work {
		public final Class<?> entityType;
		public final Object entity;
		public final Object providedId;
		public final SearchITHelper.WorkType workType;

		public Work(Class<?> entityType, Object providedId, WorkType workType) {
			this.entityType = entityType;
			this.entity = null;
			this.providedId = providedId;
			this.workType = workType;
		}

		public Work(Object entity, Object providedId, WorkType workType) {
			this.entityType = entity.getClass();
			this.entity = entity;
			this.providedId = providedId;
			this.workType = workType;
		}
	}

	private enum WorkType {
		ADD,

		UPDATE,

		DELETE,

		/**
		 * This type is used for batch indexing.
		 */
		INDEX
	}

	public class EntityTypeWorkContext {
		private final WorkType workType;

		private final WorkExecutor executor;

		private EntityTypeWorkContext(WorkType workType, WorkExecutor executor) {
			super();
			this.workType = workType;
			this.executor = executor;
		}

		public EntityTypeWorkContext push(Class<?> type, Serializable... ids) {
			return push( type, Arrays.stream( ids ) );
		}

		public EntityTypeWorkContext push(Class<?> type, Iterable<? extends Serializable> ids) {
			return push( type, StreamSupport.stream( ids.spliterator(), false ) );
		}

		public EntityTypeWorkContext push(Class<?> type, Stream<? extends Serializable> ids) {
			executor.push( ids.map( id -> new Work( type, id, workType ) ) );
			return this;
		}

		public void execute() {
			executor.execute();
		}
	}

	public abstract class AssertHSQueryContext {

		private String description = null;

		protected abstract HSQuery getHSQuery();

		public AssertHSQueryContext as(String description) {
			this.description = description;
			return this;
		}

		public ListAssert asResultIds() {
			HSQuery hsQuery = getHSQuery();
			hsQuery.projection( ProjectionConstants.ID );
			List<Object[]> results = (List<Object[]>) hsQuery.fetch();
			List<Object> ids = results.stream()
					.map( array -> array[0] )
					.collect( Collectors.toList() );
			return assertThat( ids )
					.as( "IDs of results of query " + toString( hsQuery ) );
		}

		public ListAssert asResultProjectionsAsLists() {
			HSQuery hsQuery = getHSQuery();
			List<Object[]> results = (List<Object[]>) hsQuery.fetch();
			List<List<Object>> projections = results.stream()
					.map( Arrays::asList ) // Take advantage of List.equals when calling ListAssert.containsExactly, for instance
					.collect( Collectors.toList() );
			return assertThat( projections )
					.as( "Projections of results of query " + toString( hsQuery ) );
		}

		public AbstractIntegerAssert<?> asResultSize() {
			HSQuery hsQuery = getHSQuery();
			int actualSize = hsQuery.getResultSize();
			return assertThat( actualSize )
					.as( "Number of results of query " + toString( hsQuery ) );
		}

		@SafeVarargs
		public final AssertHSQueryContext matchesExactlyIds(Serializable... expectedIds) {
			Object[] objectArray = Arrays.stream( expectedIds ).toArray();
			asResultIds().containsExactly( objectArray );
			return this;
		}

		public final AssertHSQueryContext matchesExactlyIds(int[] expectedIds) {
			Object[] objectArray = Arrays.stream( expectedIds ).mapToObj( i -> i ).toArray();
			asResultIds().containsExactly( objectArray );
			return this;
		}

		@SafeVarargs
		public final AssertHSQueryContext matchesUnorderedIds(Serializable... expectedIds) {
			Object[] objectArray = Arrays.stream( expectedIds ).toArray();
			asResultIds().containsExactlyInAnyOrder( objectArray );
			return this;
		}

		public final AssertHSQueryContext matchesUnorderedIds(int[] expectedIds) {
			Object[] objectArray = Arrays.stream( expectedIds ).mapToObj( i -> i ).toArray();
			asResultIds().containsExactlyInAnyOrder( objectArray );
			return this;
		}

		@SafeVarargs
		public final AssertHSQueryContext matchesExactlyProjections(Object[]... expectedProjections) {
			Object[] objectArray = Arrays.stream( expectedProjections )
					.map( Arrays::asList ) // Take advantage of List.equals
					.toArray();
			asResultProjectionsAsLists().containsExactly( objectArray );
			return this;
		}

		@SafeVarargs
		public final <T> AssertHSQueryContext matchesExactlySingleProjections(T... expectedSingleElementProjections) {
			Object[] objectArray = Arrays.stream( expectedSingleElementProjections )
					.map( p -> Arrays.asList( p ) ) // Take advantage of List.equals
					.toArray();
			asResultProjectionsAsLists().containsExactly( objectArray );
			return this;
		}

		@SafeVarargs
		public final AssertHSQueryContext matchesUnorderedProjections(Object[]... expectedProjections) {
			Object[] objectArray = Arrays.stream( expectedProjections )
					.map( Arrays::asList ) // Take advantage of List.equals
					.toArray();
			asResultProjectionsAsLists().containsExactlyInAnyOrder( objectArray );
			return this;
		}

		@SafeVarargs
		public final <T> AssertHSQueryContext matchesUnorderedSingleProjections(T... expectedSingleElementProjections) {
			Object[] objectArray = Arrays.stream( expectedSingleElementProjections )
					.map( p -> Arrays.asList( p ) ) // Take advantage of List.equals
					.toArray();
			asResultProjectionsAsLists().containsExactlyInAnyOrder( objectArray );
			return this;
		}

		public final <T> AssertHSQueryContext matchesNone() {
			asResultIds().isEmpty();
			return this;
		}

		public final <T> AssertHSQueryContext matches() {
			asResultIds().isNotEmpty();
			return this;
		}

		public AssertHSQueryContext hasResultSize(int size) {
			asResultSize().isEqualTo( size );
			return this;
		}

		public final <T> AssertFacetContext facets(String facetingRequestName) {
			HSQuery hsQuery = getHSQuery();
			List<Facet> facets = hsQuery.getFacetManager().getFacets( facetingRequestName );
			return new AssertFacetContext( this, facetingRequestName, facets );
		}

		private String toString(HSQuery query) {
			StringBuilder builder = new StringBuilder();
			if ( StringHelper.isNotEmpty( description ) ) {
				builder.append( description ).append( " - " );
			}
			builder.append( "<" ).append( query.getQueryString() ).append( ">" )
					.append( " from <" ).append( query.getTargetedEntities() ).append( ">" );
			String[] projected = query.getProjectedFields();
			if ( projected != null && projected.length > 0 ) {
				builder.append( " with projections <" ).append( Arrays.toString( query.getProjectedFields() ) ).append( ">" );
			}
			return builder.toString();
		}
	}

	public class AssertFacetContext {

		private final AssertHSQueryContext queryContext;
		private final String facetingRequestName;
		private final List<Facet> allFacets;
		private final List<Facet> unmatchedFacets;

		private AssertFacetContext(AssertHSQueryContext queryContext, String facetingRequestName, List<Facet> facets) {
			this.queryContext = queryContext;
			this.facetingRequestName = facetingRequestName;
			this.allFacets = facets;
			this.unmatchedFacets = new ArrayList<>( facets );
		}

		public AssertFacetContext isEmpty() {
			assertThat( allFacets )
					.as( "Facets for faceting request '" + facetingRequestName + "' on query " + queryContext )
					.isEmpty();
			return this;
		}

		public AssertFacetContext includes(String value, int count) {
			ListIterator<Facet> it = unmatchedFacets.listIterator();
			boolean found = false;
			while ( it.hasNext() && !found ) {
				Facet facet = it.next();
				if ( Objects.equals( value, facet.getValue() ) ) {
					assertThat( facet.getCount() )
							.as( "Count for faceting request '" + facetingRequestName + "', facet '" + value + "' on query "
									+ queryContext )
							.isEqualTo( count );
					it.remove();
					found = true;
				}
			}
			if ( !found ) {
				fail( "Could not find facet '" + value + "' for faceting request '" + facetingRequestName + "' on query "
						+ queryContext );
			}
			return this;
		}

		/**
		 * To be called after "includes", to check that there isn't any other facet.
		 * @return This object, for chained calls.
		 */
		public AssertFacetContext only() {
			assertThat( unmatchedFacets )
					.as( "Unexpected facets for faceting request '" + facetingRequestName + "' on query " + queryContext )
					.isEmpty();
			return this;
		}

	}

	public class AssertBuildingHSQueryContext extends AssertHSQueryContext {
		private final Query luceneQuery;
		private Class<?>[] classes;
		private Consumer<HSQuery> before = q -> {};

		private AssertBuildingHSQueryContext(Query luceneQuery) {
			super();
			this.luceneQuery = luceneQuery;
		}

		public AssertBuildingHSQueryContext from(Class<?>... classes) {
			this.classes = classes;
			return this;
		}

		public AssertBuildingHSQueryContext sort(Sort sort) {
			before = before.andThen( q -> q.sort( sort ) );
			return this;
		}

		public AssertBuildingHSQueryContext projecting(String... projections) {
			before = before.andThen( q -> q.projection( projections ) );
			return this;
		}

		@Override
		protected HSQuery getHSQuery() {
			HSQuery hsQuery = sfHolder.getSearchFactory().createHSQuery( luceneQuery, session(), null, classes );
			before.accept( hsQuery );
			return hsQuery;
		}
	}

	private static Query termQuery(String fieldName, String value) {
		return new TermQuery( new Term( fieldName, value ) );
	}

}
