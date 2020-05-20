/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.ManagedAssert.assertThatManaged;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test setting an entity graph on entity loading options when executing a search query
 * when only a single type is involved.
 */
@RunWith(Parameterized.class)
public class SearchQueryEntityLoadingGraphIT<T> extends AbstractSearchQueryEntityLoadingSingleTypeIT<T> {

	@Parameterized.Parameters(name = "{0}")
	public static List<SingleTypeLoadingModelPrimitives<?>> data() {
		return allSingleTypeLoadingModelPrimitives();
	}

	private SessionFactory sessionFactory;

	public SearchQueryEntityLoadingGraphIT(SingleTypeLoadingModelPrimitives<T> primitives) {
		super( primitives );
	}

	@Before
	public void setup() {
		backendMock.expectAnySchema( primitives.getIndexName() );

		sessionFactory = ormSetupHelper.start().setup( primitives.getEntityClasses() );

		backendMock.verifyExpectationsMet();

		// We don't care about what is indexed exactly, so use the lenient mode
		backendMock.inLenientMode( () -> withinTransaction( sessionFactory(), session -> {
			session.persist( primitives.newIndexedWithContained( 0 ) );
			session.persist( primitives.newIndexedWithContained( 1 ) );
			session.persist( primitives.newIndexedWithContained( 2 ) );
		} ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void defaults() {
		testLoadingWithEntityGraph(
				// Do not use any graph
				null, null,
				// The eager association is loaded, but not the lazy one
				true, false
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void eager_fetch() {
		testLoadingWithEntityGraph(
				// Use a graph that forces eager loading of all associations
				// with FETCH semantic, meaning default EAGERs are overridden.
				primitives.getEagerGraphName(), GraphSemantic.FETCH,
				// Both associations are loaded
				true, true
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void eager_load() {
		testLoadingWithEntityGraph(
				// Use a graph that forces eager loading of all associations
				// with LOAD semantic, meaning default EAGERs are NOT overridden.
				primitives.getEagerGraphName(), GraphSemantic.LOAD,
				// Both associations are loaded
				true, true
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void lazy_fetch() {
		testLoadingWithEntityGraph(
				// Use a graph that doesn't force loading of any association,
				// with FETCH semantic, meaning default EAGERs are overridden.
				primitives.getLazyGraphName(), GraphSemantic.FETCH,
				// Neither association is loaded
				false, false
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void lazy_load() {
		testLoadingWithEntityGraph(
				// Use a "lazy" graph that doesn't force loading of any association,
				// with LOAD semantic, meaning default EAGERs are NOT overridden.
				primitives.getLazyGraphName(), GraphSemantic.LOAD,
				// The eager association is loaded, but not the lazy one
				true, false
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graphName_null() {
		assertThatThrownBy( () -> OrmUtils.withinSession( sessionFactory(), session -> {
			Search.session( session ).search( primitives.getIndexedClass() )
					.where( f -> f.matchAll() )
					.loading( o -> o.graph( (String) null, GraphSemantic.FETCH ) )
					.toQuery();
		} ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'graphName' must not be null" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graphName_invalid() {
		assertThatThrownBy( () -> OrmUtils.withinSession( sessionFactory(), session -> {
			Search.session( session ).search( primitives.getIndexedClass() )
					.where( f -> f.matchAll() )
					.loading( o -> o.graph( "invalidGraphName", GraphSemantic.FETCH ) )
					.toQuery();
		} ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll( "Could not locate EntityGraph with given name", "invalidGraphName" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graphName_graphSemantic_null() {
		assertThatThrownBy( () -> OrmUtils.withinSession( sessionFactory(), session -> {
			Search.session( session ).search( primitives.getIndexedClass() )
					.where( f -> f.matchAll() )
					.loading( o -> o.graph( primitives.getEagerGraphName(), null ) )
					.toQuery();
		} ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'semantic' must not be null" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graph_null() {
		assertThatThrownBy( () -> OrmUtils.withinSession( sessionFactory(), session -> {
			Search.session( session ).search( primitives.getIndexedClass() )
					.where( f -> f.matchAll() )
					.loading( o -> o.graph( (RootGraph<?>) null, GraphSemantic.FETCH ) )
					.toQuery();
		} ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'graph' must not be null" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graph_graphSemantic_null() {
		assertThatThrownBy( () -> OrmUtils.withinSession( sessionFactory(), session -> {
			Search.session( session ).search( primitives.getIndexedClass() )
					.where( f -> f.matchAll() )
					.loading( o -> o.graph( session.getEntityGraph( primitives.getEagerGraphName() ), null ) )
					.toQuery();
		} ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'semantic' must not be null" );
	}

	@Override
	protected SessionFactory sessionFactory() {
		return sessionFactory;
	}

	private void testLoadingWithEntityGraph(String graphName, GraphSemantic graphSemantic,
			boolean expectEagerAssociationLoaded, boolean expectLazyAssociationLoaded) {
		testLoading(
				session -> { }, // No particular session setup
				o -> {
					if ( graphName != null || graphSemantic != null ) {
						o.graph( graphName, graphSemantic );
					}
				},
				c -> c
						.doc( primitives.getIndexName(), primitives.getDocumentIdForEntityId( 0 ) )
						.doc( primitives.getIndexName(), primitives.getDocumentIdForEntityId( 1 ) )
						.doc( primitives.getIndexName(), primitives.getDocumentIdForEntityId( 2 ) ),
				c -> c
						.entity( primitives.getIndexedClass(), 0 )
						.entity( primitives.getIndexedClass(), 1 )
						.entity( primitives.getIndexedClass(), 2 ),
				(assertions, loadedList) -> assertions.assertThat( loadedList )
						.isNotEmpty()
						.allSatisfy( loaded -> assertThatManaged( primitives.getContainedEager( loaded ) )
								.as( "Eager contained for " + loaded )
								.isInitialized( expectEagerAssociationLoaded ) )
						.allSatisfy( loaded -> assertThatManaged( primitives.getContainedLazy( loaded ) )
								.as( "Lazy contained for " + loaded )
								.isInitialized( expectLazyAssociationLoaded ) )
		);
	}
}
