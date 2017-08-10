/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.junit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.fest.assertions.Assertions;
import org.fest.assertions.IntAssert;
import org.fest.assertions.ListAssert;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.hibernate.search.util.StringHelper;

/**
 * A helper for Hibernate Search integration tests.
 * <p>
 * Provides easy-to-use primitives frequently used in integration tests.
 *
 * @author Yoann Rodiere
 */
public class SearchITHelper {

	private final Supplier<? extends SearchIntegrator> integratorProvider;

	public SearchITHelper(SearchFactoryHolder sfHolder) {
		this( sfHolder::getSearchFactory );
	}

	public SearchITHelper(Supplier<? extends SearchIntegrator> integratorProvider) {
		super();
		this.integratorProvider = integratorProvider;
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

	public void add(Object ... entries) {
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

	public void index(Object ... entries) {
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

	public void delete(Class<?> type, Serializable ... ids) {
		delete().push( type, ids ).execute();
	}

	public QueryBuilder queryBuilder(Class<?> clazz) {
		return integratorProvider.get().buildQueryBuilder().forEntity( clazz ).get();
	}

	public HSQuery hsQuery(Class<?> ... classes) {
		return hsQuery( new MatchAllDocsQuery(), classes );
	}

	public HSQuery hsQuery(Query query, Class<?> ... classes) {
		return integratorProvider.get().createHSQuery( query, classes );
	}

	public AssertBuildingHSQueryContext assertThat(String fieldName, String value) {
		return assertThat( termQuery( fieldName, value ) );
	}

	public AssertBuildingHSQueryContext assertThat() {
		return assertThat( new MatchAllDocsQuery() );
	}

	public AssertBuildingHSQueryContext assertThat(Query luceneQuery) {
		return new AssertBuildingHSQueryContext( luceneQuery );
	}

	public AssertHSQueryContext assertThat(HSQuery hsQuery) {
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
			TransactionContextForTest tc = new TransactionContextForTest();
			works.forEach(
					w -> integratorProvider.get().getWorker().performWork( w, tc )
			);
			tc.end();
			works.clear();
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
			executor.push( new Work( executor.tenantId, entry, id, workType, false ) );
			return this;
		}

		public EntityInstanceWorkContext push(Iterable<?> entries) {
			return push( StreamSupport.stream( entries.spliterator(), false ) );
		}

		public EntityInstanceWorkContext push(Object ... entries) {
			return push( Arrays.stream( entries ) );
		}

		public EntityInstanceWorkContext push(Stream<?> entries) {
			executor.push( entries.map( e -> new Work( executor.tenantId, e, null, workType, false ) ) );
			return this;
		}

		public void execute() {
			executor.execute();
		}
	}

	public class EntityTypeWorkContext {
		private final WorkType workType;

		private WorkExecutor executor;

		private EntityTypeWorkContext(WorkType workType, WorkExecutor executor) {
			super();
			this.workType = workType;
			this.executor = executor;
		}

		public EntityTypeWorkContext push(Class<?> type, Serializable ... ids) {
			return push( type, Arrays.stream( ids ) );
		}

		public EntityTypeWorkContext push(Class<?> type, Iterable<? extends Serializable> ids) {
			return push( type, StreamSupport.stream( ids.spliterator(), false ) );
		}

		public EntityTypeWorkContext push(Class<?> type, Stream<? extends Serializable> ids) {
			executor.push( ids.map( id -> new Work( executor.tenantId, new PojoIndexedTypeIdentifier( type ), id, workType ) ) );
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
			List<EntityInfo> results = hsQuery.queryEntityInfos();
			List<Serializable> ids = results.stream()
					.map( EntityInfo::getId )
					.collect( Collectors.toList() );
			return Assertions.assertThat( ids )
					.as( "IDs of results of query " + toString( hsQuery ) );
		}

		public ListAssert asResultProjectionsAsLists() {
			HSQuery hsQuery = getHSQuery();
			List<EntityInfo> results = hsQuery.queryEntityInfos();
			List<List<Object>> projections = results.stream()
					.map( EntityInfo::getProjection )
					.map( Arrays::asList ) // Take advantage of List.equals when calling ListAssert.containsExactly, for instance
					.collect( Collectors.toList() );
			return Assertions.assertThat( projections )
					.as( "Projections of results of query " + toString( hsQuery ) );
		}

		public ListAssert asResults() {
			HSQuery hsQuery = getHSQuery();
			List<EntityInfo> results = hsQuery.queryEntityInfos();
			return Assertions.assertThat( results )
					.as( "Results of query " + toString( hsQuery ) );
		}

		public IntAssert asResultSize() {
			HSQuery hsQuery = getHSQuery();
			int actualSize = hsQuery.queryResultSize();
			return Assertions.assertThat( actualSize )
					.as( "Number of results of query " + toString( hsQuery ) );
		}

		@SafeVarargs
		public final AssertHSQueryContext matchesExactlyIds(Serializable ... expectedIds) {
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
		public final AssertHSQueryContext matchesUnorderedIds(Serializable ... expectedIds) {
			Object[] objectArray = Arrays.stream( expectedIds ).toArray();
			asResultIds().containsOnly( objectArray );
			return this;
		}

		public final AssertHSQueryContext matchesUnorderedIds(int[] expectedIds) {
			Object[] objectArray = Arrays.stream( expectedIds ).mapToObj( i -> i ).toArray();
			asResultIds().containsOnly( objectArray );
			return this;
		}

		@SafeVarargs
		public final AssertHSQueryContext matchesExactlyProjections(Object[] ... expectedProjections) {
			Object[] objectArray = Arrays.stream( expectedProjections )
					.map( Arrays::asList ) // Take advantage of List.equals
					.toArray();
			asResultProjectionsAsLists().containsExactly( objectArray );
			return this;
		}

		@SafeVarargs
		public final <T> AssertHSQueryContext matchesExactlySingleProjections(T ... expectedSingleElementProjections) {
			Object[] objectArray = Arrays.stream( expectedSingleElementProjections )
					.map( p -> Arrays.asList( p ) ) // Take advantage of List.equals
					.toArray();
			asResultProjectionsAsLists().containsExactly( objectArray );
			return this;
		}

		@SafeVarargs
		public final AssertHSQueryContext matchesUnorderedProjections(Object[] ... expectedProjections) {
			Object[] objectArray = Arrays.stream( expectedProjections )
					.map( Arrays::asList ) // Take advantage of List.equals
					.toArray();
			asResultProjectionsAsLists().containsOnly( objectArray );
			return this;
		}

		@SafeVarargs
		public final <T> AssertHSQueryContext matchesUnorderedSingleProjections(T ... expectedSingleElementProjections) {
			Object[] objectArray = Arrays.stream( expectedSingleElementProjections )
					.map( p -> Arrays.asList( p ) ) // Take advantage of List.equals
					.toArray();
			asResultProjectionsAsLists().containsOnly( objectArray );
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
				builder.append( " with projections <" ).append( query.getProjectedFields() ).append( ">" );
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
			Assertions.assertThat( allFacets )
					.as( "Facets for faceting request '" + facetingRequestName + "' on query " + queryContext )
					.isEmpty();
			return this;
		}

		public AssertFacetContext includes(String value, int count) {
			ListIterator<Facet> it = unmatchedFacets.listIterator();
			while ( it.hasNext() ) {
				Facet facet = it.next();
				if ( Objects.equals( value, facet.getValue() ) ) {
					Assertions.assertThat( facet.getCount() )
							.as( "Count for faceting request '" + facetingRequestName + "', facet '" + value + "' on query " + queryContext )
							.isEqualTo( count );
					it.remove();
				}
			}
			return this;
		}

		/**
		 * To be called after "includes", to check that there isn't any other facet.
		 * @return This object, for chained calls.
		 */
		public AssertFacetContext only() {
			Assertions.assertThat( unmatchedFacets )
					.as( "Unexpected facets for faceting request '" + facetingRequestName + "' on query " + queryContext )
					.isEmpty();
			return this;
		}

	}

	public class AssertBuildingHSQueryContext extends AssertHSQueryContext {
		private final Query luceneQuery;
		private Class<?>[] classes;
		private Consumer<HSQuery> before = q -> { };

		private AssertBuildingHSQueryContext(Query luceneQuery) {
			super();
			this.luceneQuery = luceneQuery;
		}

		public AssertBuildingHSQueryContext from(Class<?> ... classes) {
			this.classes = classes;
			return this;
		}

		public AssertBuildingHSQueryContext sort(Sort sort) {
			before = before.andThen( q -> q.sort( sort ) );
			return this;
		}

		public AssertBuildingHSQueryContext projecting(String ... projections) {
			before = before.andThen( q -> q.projection( projections ) );
			return this;
		}

		@Override
		protected HSQuery getHSQuery() {
			HSQuery hsQuery = integratorProvider.get().createHSQuery( luceneQuery, classes );
			before.accept( hsQuery );
			return hsQuery;
		}
	}

	private static Query termQuery(String fieldName, String value) {
		return new TermQuery( new Term( fieldName, value ) );
	}

}
