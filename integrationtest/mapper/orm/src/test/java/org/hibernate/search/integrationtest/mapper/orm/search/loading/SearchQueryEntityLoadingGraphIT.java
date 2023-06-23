/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.ManagedAssert.assertThatManaged;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SingleTypeLoadingMapping;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SingleTypeLoadingModel;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test setting an entity graph on entity loading options when executing a search query
 * when only a single type is involved.
 */
@RunWith(Parameterized.class)
public class SearchQueryEntityLoadingGraphIT<T> extends AbstractSearchQueryEntityLoadingSingleTypeIT<T> {

	@Parameterized.Parameters(name = "{0}, {1}")
	public static List<Object[]> params() {
		List<Object[]> result = new ArrayList<>();
		forAllModelMappingCombinations( (model, mapping) -> {
			result.add( new Object[] { model, mapping } );
		} );
		return result;
	}

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	public SearchQueryEntityLoadingGraphIT(SingleTypeLoadingModel<T> model, SingleTypeLoadingMapping mapping) {
		super( model, mapping );
	}

	@Override
	protected BackendMock backendMock() {
		return backendMock;
	}

	@Override
	protected SessionFactory sessionFactory() {
		return setupHolder.sessionFactory();
	}

	@ReusableOrmSetupHolder.SetupParams
	public List<?> setupParams() {
		return Arrays.asList( mapping, model );
	}

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext, ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		backendMock.expectAnySchema( model.getIndexName() );
		setupContext.withConfiguration( c -> mapping.configure( c, model ) );

		dataClearConfig.preClear( model.getIndexedClass(), model::clearContainedEager );
		dataClearConfig.clearOrder( model.getContainedClass(), model.getIndexedClass() );
	}

	@Before
	public void initData() {
		// We don't care about what is indexed exactly, so use the lenient mode
		backendMock.inLenientMode( () -> with( sessionFactory() ).runInTransaction( session -> {
			session.persist( model.newIndexedWithContained( 0, mapping ) );
			session.persist( model.newIndexedWithContained( 1, mapping ) );
			session.persist( model.newIndexedWithContained( 2, mapping ) );
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
				model.getEagerGraphName(), GraphSemantic.FETCH,
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
				model.getEagerGraphName(), GraphSemantic.LOAD,
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
				model.getLazyGraphName(), GraphSemantic.FETCH,
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
				model.getLazyGraphName(), GraphSemantic.LOAD,
				// The eager association is loaded, but not the lazy one
				true, false
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graphName_null() {
		assertThatThrownBy( () -> with( sessionFactory() ).runNoTransaction(
				session -> Search.session( session ).search( model.getIndexedClass() )
						.where( f -> f.matchAll() )
						.loading( o -> o.graph( (String) null, GraphSemantic.FETCH ) )
						.toQuery()
		) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'graphName' must not be null" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graphName_invalid() {
		assertThatThrownBy( () -> with( sessionFactory() ).runNoTransaction(
				session -> Search.session( session ).search( model.getIndexedClass() )
						.where( f -> f.matchAll() )
						.loading( o -> o.graph( "invalidGraphName", GraphSemantic.FETCH ) )
						.toQuery()
		) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll( "Could not locate EntityGraph with given name", "invalidGraphName" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graphName_graphSemantic_null() {
		assertThatThrownBy( () -> with( sessionFactory() ).runNoTransaction(
				session -> Search.session( session ).search( model.getIndexedClass() )
						.where( f -> f.matchAll() )
						.loading( o -> o.graph( model.getEagerGraphName(), null ) )
						.toQuery()
		) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'semantic' must not be null" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graph_null() {
		assertThatThrownBy( () -> with( sessionFactory() ).runNoTransaction(
				session -> Search.session( session ).search( model.getIndexedClass() )
						.where( f -> f.matchAll() )
						.loading( o -> o.graph( (RootGraph<?>) null, GraphSemantic.FETCH ) )
						.toQuery()
		) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'graph' must not be null" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graph_graphSemantic_null() {
		assertThatThrownBy( () -> with( sessionFactory() ).runNoTransaction(
				session -> Search.session( session ).search( model.getIndexedClass() )
						.where( f -> f.matchAll() )
						.loading( o -> o.graph( session.getEntityGraph( model.getEagerGraphName() ), null ) )
						.toQuery()
		) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'semantic' must not be null" );
	}

	private void testLoadingWithEntityGraph(String graphName, GraphSemantic graphSemantic,
			boolean expectEagerAssociationLoaded, boolean expectLazyAssociationLoaded) {
		testLoading(
				session -> {}, // No particular session setup
				o -> {
					if ( graphName != null || graphSemantic != null ) {
						o.graph( graphName, graphSemantic );
					}
				},
				c -> c
						.doc( model.getIndexName(), mapping.getDocumentIdForEntityId( 0 ) )
						.doc( model.getIndexName(), mapping.getDocumentIdForEntityId( 1 ) )
						.doc( model.getIndexName(), mapping.getDocumentIdForEntityId( 2 ) ),
				c -> c
						.entity( model.getIndexedClass(), 0 )
						.entity( model.getIndexedClass(), 1 )
						.entity( model.getIndexedClass(), 2 ),
				(assertions, loadedList) -> assertions.assertThat( loadedList )
						.isNotEmpty()
						.allSatisfy( loaded -> assertThatManaged( model.getContainedEager( loaded ) )
								.as( "Eager contained for " + loaded )
								.isInitialized( expectEagerAssociationLoaded ) )
						.allSatisfy( loaded -> assertThatManaged( model.getContainedLazy( loaded ) )
								.as( "Lazy contained for " + loaded )
								.isInitialized( expectLazyAssociationLoaded ) )
		);
	}
}
