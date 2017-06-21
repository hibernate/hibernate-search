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
		return sfHolder.getSearchFactory().buildQueryBuilder().forEntity( clazz ).get();
	}

	public HSQuery hsQuery(Class<?> ... classes) {
		return hsQuery( new MatchAllDocsQuery(), classes );
	}

	public HSQuery hsQuery(Query query, Class<?> ... classes) {
		return sfHolder.getSearchFactory().createHSQuery( query, classes );
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

	public AssertHSQueryContext assertThat(final HSQuery hsQuery) {
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

		public void execute() {
			TransactionContextForTest tc = new TransactionContextForTest();
			for ( Work work : works ) {
					sfHolder.getSearchFactory().getWorker().performWork( work, tc );
			}
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
			for ( Object entry : entries ) {
				executor.push( new Work( executor.tenantId, entry, null, workType, false ) );
			}
			return this;
		}

		public EntityInstanceWorkContext push(Object ... entries) {
			return push( Arrays.asList( entries ) );
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
			return push( type, Arrays.asList( ids ) );
		}

		public EntityTypeWorkContext push(Class<?> type, Iterable<? extends Serializable> ids) {
			for ( Serializable id : ids ) {
				executor.push( new Work( executor.tenantId, type, id, workType ) );
			}
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
			List<Serializable> ids = new ArrayList<>();
			for ( EntityInfo info : results ) {
				ids.add( info.getId() );
			}
			return Assertions.assertThat( ids )
					.as( "IDs of results of query " + toString( hsQuery ) );
		}

		public ListAssert asResultProjectionsAsLists() {
			HSQuery hsQuery = getHSQuery();
			List<EntityInfo> results = hsQuery.queryEntityInfos();
			List<List<Object>> projections = new ArrayList<>();
			for ( EntityInfo info : results ) {
				// Take advantage of List.equals when calling ListAssert.containsExactly, for instance
				List<Object> projectionAsList = Arrays.asList( info.getProjection() );
				projections.add( projectionAsList );
			}
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
			Object[] objectArray = new Object[expectedIds.length];
			for ( int i = 0; i < expectedIds.length; ++i ) {
				objectArray[i] = expectedIds[i];
			}
			asResultIds().containsExactly( objectArray );
			return this;
		}

		public final AssertHSQueryContext matchesExactlyIds(int[] expectedIds) {
			Object[] objectArray = new Object[expectedIds.length];
			for ( int i = 0; i < expectedIds.length; ++i ) {
				objectArray[i] = expectedIds[i];
			}
			asResultIds().containsExactly( objectArray );
			return this;
		}

		@SafeVarargs
		public final AssertHSQueryContext matchesUnorderedIds(Serializable ... expectedIds) {
			Object[] objectArray = new Object[expectedIds.length];
			for ( int i = 0; i < expectedIds.length; ++i ) {
				objectArray[i] = expectedIds[i];
			}
			asResultIds().containsOnly( objectArray );
			return this;
		}

		public final AssertHSQueryContext matchesUnorderedIds(int[] expectedIds) {
			Object[] objectArray = new Object[expectedIds.length];
			for ( int i = 0; i < expectedIds.length; ++i ) {
				objectArray[i] = expectedIds[i];
			}
			asResultIds().containsOnly( objectArray );
			return this;
		}

		@SafeVarargs
		public final AssertHSQueryContext matchesExactlyProjections(Object[] ... expectedProjections) {
			Object[] objectArray = new Object[expectedProjections.length];
			for ( int i = 0; i < expectedProjections.length; ++i ) {
				// Take advantage of List.equals when calling ListAssert.containsExactly
				List<Object> projectionAsList = Arrays.asList( expectedProjections[i] );
				objectArray[i] = projectionAsList;
			}
			asResultProjectionsAsLists().containsExactly( objectArray );
			return this;
		}

		@SafeVarargs
		public final <T> AssertHSQueryContext matchesExactlySingleProjections(T ... expectedSingleElementProjections) {
			Object[] objectArray = new Object[expectedSingleElementProjections.length];
			for ( int i = 0; i < expectedSingleElementProjections.length; ++i ) {
				// Take advantage of List.equals when calling ListAssert.containsExactly
				List<T> projectionAsList = Arrays.asList( expectedSingleElementProjections[i] );
				objectArray[i] = projectionAsList;
			}
			asResultProjectionsAsLists().containsExactly( objectArray );
			return this;
		}

		@SafeVarargs
		public final AssertHSQueryContext matchesUnorderedProjections(Object[] ... expectedProjections) {
			Object[] objectArray = new Object[expectedProjections.length];
			for ( int i = 0; i < expectedProjections.length; ++i ) {
				// Take advantage of List.equals when calling ListAssert.containsOnly
				List<Object> projectionAsList = Arrays.asList( expectedProjections[i] );
				objectArray[i] = projectionAsList;
			}
			asResultProjectionsAsLists().containsOnly( objectArray );
			return this;
		}

		@SafeVarargs
		public final <T> AssertHSQueryContext matchesUnorderedSingleProjections(T ... expectedSingleElementProjections) {
			Object[] objectArray = new Object[expectedSingleElementProjections.length];
			for ( int i = 0; i < expectedSingleElementProjections.length; ++i ) {
				// Take advantage of List.equals when calling ListAssert.containsOnly
				List<T> projectionAsList = Arrays.asList( expectedSingleElementProjections[i] );
				objectArray[i] = projectionAsList;
			}
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

	public class AssertBuildingHSQueryContext extends AssertHSQueryContext {
		private final Query luceneQuery;
		private Class<?>[] classes;
		private List<Consumer<HSQuery>> before = new ArrayList<>();

		private AssertBuildingHSQueryContext(Query luceneQuery) {
			super();
			this.luceneQuery = luceneQuery;
		}

		public AssertBuildingHSQueryContext from(Class<?> ... classes) {
			this.classes = classes;
			return this;
		}

		public AssertBuildingHSQueryContext sort(final Sort sort) {
			before.add( new Consumer<HSQuery>() {
				@Override
				public void accept(HSQuery param) {
					param.sort( sort );
				}
			} );
			return this;
		}

		public AssertBuildingHSQueryContext projecting(final String ... projections) {
			before.add( new Consumer<HSQuery>() {
				@Override
				public void accept(HSQuery param) {
					param.projection( projections );
				}
			} );
			return this;
		}

		@Override
		protected HSQuery getHSQuery() {
			HSQuery hsQuery = sfHolder.getSearchFactory().createHSQuery( luceneQuery, classes );
			for ( Consumer<HSQuery> consumer : before ) {
				consumer.accept( hsQuery );
			}
			return hsQuery;
		}
	}

	private static Query termQuery(String fieldName, String value) {
		return new TermQuery( new Term( fieldName, value ) );
	}

	private interface Consumer<T> {
		void accept(T param);
	}

}
