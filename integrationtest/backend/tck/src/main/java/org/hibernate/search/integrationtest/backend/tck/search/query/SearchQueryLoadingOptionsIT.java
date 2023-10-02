/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubEntity;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.GenericStubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@TestForIssue(jiraKey = "HSEARCH-3988")
@SuppressWarnings("unchecked") // Mocking parameterized types
class SearchQueryLoadingOptionsIT {

	private static final ProjectionMappedTypeContext typeContextMock = Mockito.mock( ProjectionMappedTypeContext.class );

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	void defaultResultType() {
		SearchLoadingContext<StubEntity> loadingContextMock = mock( SearchLoadingContext.class );
		Consumer<Object> loadingOptionsStepMock = mock( Consumer.class );

		Object someOption = new Object();

		when( typeContextMock.loadingAvailable() ).thenReturn( true );

		index.mapping().with()
				.typeContext( index.typeName(), typeContextMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope =
							index.createGenericScope( loadingContextMock );
					scope.query( loadingOptionsStepMock )
							.where( f -> f.matchAll() )
							.loading( o -> o.accept( someOption ) )
							.toQuery();
				} );
		// Expect our loading options to be altered
		verify( loadingOptionsStepMock ).accept( someOption );
	}

	@Test
	void selectEntity() {
		SearchLoadingContext<StubEntity> loadingContextMock = mock( SearchLoadingContext.class );
		Consumer<Object> loadingOptionsStepMock = mock( Consumer.class );

		Object someOption = new Object();

		when( typeContextMock.loadingAvailable() ).thenReturn( true );

		index.mapping().with()
				.typeContext( index.typeName(), typeContextMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope =
							index.createGenericScope( loadingContextMock );
					scope.query( loadingOptionsStepMock )
							.selectEntity()
							.where( f -> f.matchAll() )
							.loading( o -> o.accept( someOption ) )
							.toQuery();
				} );
		// Expect our loading options to be altered
		verify( loadingOptionsStepMock ).accept( someOption );
	}

	@Test
	void selectEntityReference() {
		SearchLoadingContext<StubEntity> loadingContextMock = mock( SearchLoadingContext.class );
		Consumer<Object> loadingOptionsStepMock = mock( Consumer.class );

		Object someOption = new Object();

		index.mapping().with()
				.typeContext( index.typeName(), typeContextMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope =
							index.createGenericScope( loadingContextMock );
					scope.query( loadingOptionsStepMock )
							.selectEntityReference()
							.where( f -> f.matchAll() )
							.loading( o -> o.accept( someOption ) )
							.toQuery();
				} );
		// Expect our loading options to be altered
		verify( loadingOptionsStepMock ).accept( someOption );
	}

	@Test
	void select() {
		SearchLoadingContext<StubEntity> loadingContextMock =
				mock( SearchLoadingContext.class );
		Consumer<Object> loadingOptionsStepMock = mock( Consumer.class );

		Object someOption = new Object();

		when( typeContextMock.loadingAvailable() ).thenReturn( true );

		index.mapping().with()
				.typeContext( index.typeName(), typeContextMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope =
							index.createGenericScope( loadingContextMock );
					scope.query( loadingOptionsStepMock )
							.select( f -> f.composite( f.entity(), f.field( "string" ) ) )
							.where( f -> f.matchAll() )
							.loading( o -> o.accept( someOption ) )
							.toQuery();
				} );
		// Expect our loading options to be altered
		verify( loadingOptionsStepMock ).accept( someOption );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) )
					.toReference();
		}
	}

}
