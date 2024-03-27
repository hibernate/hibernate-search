/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.ManagedAssert.assertThatManaged;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SingleTypeLoadingMapping;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SingleTypeLoadingModel;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedPerClass;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetup;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetupBeforeTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test setting an entity graph on entity loading options when executing a search query
 * when only a single type is involved.
 */
@ParameterizedPerClass
public class SearchQueryEntityLoadingGraphIT<T> extends AbstractSearchQueryEntityLoadingSingleTypeIT<T> {

	public static List<? extends Arguments> params() {
		List<Arguments> result = new ArrayList<>();
		forAllModelMappingCombinations( (model, mapping) -> {
			result.add( Arguments.of( model, mapping ) );
		} );
		return result;
	}

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;
	private SingleTypeLoadingModel<T> model;
	private SingleTypeLoadingMapping mapping;

	@Override
	protected BackendMock backendMock() {
		return backendMock;
	}

	@Override
	protected SessionFactory sessionFactory() {
		return sessionFactory;
	}

	@Override
	protected SingleTypeLoadingModel<T> model() {
		return model;
	}

	@Override
	protected SingleTypeLoadingMapping mapping() {
		return mapping;
	}

	@ParameterizedSetup
	@MethodSource("params")
	void setup(SingleTypeLoadingModel<T> model, SingleTypeLoadingMapping mapping) {
		this.model = model;
		this.mapping = mapping;
		backendMock.expectAnySchema( model.getIndexName() );
		sessionFactory = ormSetupHelper.start().withConfiguration( c -> mapping.configure( c, model ) )
				.dataClearing( true, config -> config
						.preClear( model.getIndexedClass(), model::clearContainedEager )
						.clearOrder( model.getContainedClass(), model.getIndexedClass() ) )
				.setup();
	}

	@ParameterizedSetupBeforeTest
	void initData() {
		// We don't care about what is indexed exactly, so use the lenient mode
		backendMock.inLenientMode( () -> with( sessionFactory() ).runInTransaction( session -> {
			session.persist( model.newIndexedWithContained( 0, mapping ) );
			session.persist( model.newIndexedWithContained( 1, mapping ) );
			session.persist( model.newIndexedWithContained( 2, mapping ) );
		} ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	void defaults() {
		testLoadingWithEntityGraph(
				// Do not use any graph
				null, null,
				// The eager association is loaded, but not the lazy one
				true, false
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	void eager_fetch() {
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
	void eager_load() {
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
	void lazy_fetch() {
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
	void lazy_load() {
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
	void graphName_null() {
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
	void graphName_invalid() {
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
	void graphName_graphSemantic_null() {
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
	void graph_null() {
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
	void graph_graphSemantic_null() {
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
