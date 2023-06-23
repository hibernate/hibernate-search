/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.GenericStubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@TestForIssue(jiraKey = "HSEARCH-3988")
@SuppressWarnings("unchecked") // Mocking parameterized types
public class SearchQueryLoadingOptionsIT {

	private static final ProjectionMappedTypeContext typeContextMock = Mockito.mock( ProjectionMappedTypeContext.class );

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void defaultResultType() {
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
	public void selectEntity() {
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
	public void selectEntityReference() {
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
	public void select() {
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
