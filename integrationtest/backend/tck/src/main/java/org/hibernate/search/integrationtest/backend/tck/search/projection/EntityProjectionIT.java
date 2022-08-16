/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.projection.definition.spi.CompositeProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubEntity;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubTransformedReference;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.GenericStubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.mockito.Mockito;

@SuppressWarnings("unchecked") // Mocking parameterized types
public class EntityProjectionIT extends AbstractEntityProjectionIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new )
			.name( "main" );
	private static final SimpleMappedIndex<IndexBinding> multiIndex1 = SimpleMappedIndex.of( IndexBinding::new )
			.name( "multi1" );
	private static final SimpleMappedIndex<IndexBinding> multiIndex2 = SimpleMappedIndex.of( IndexBinding::new )
			.name( "multi2" );
	private static final SimpleMappedIndex<IndexBinding> multiIndex3 = SimpleMappedIndex.of( IndexBinding::new )
			.name( "multi3" );
	private static final SimpleMappedIndex<IndexBinding> multiIndex4 = SimpleMappedIndex.of( IndexBinding::new )
			.name( "multi4" );

	public EntityProjectionIT() {
		super( mainIndex, multiIndex1, multiIndex2, multiIndex3, multiIndex4 );
	}

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndexes( mainIndex, multiIndex1, multiIndex2, multiIndex3, multiIndex4 ).setup();

		final BulkIndexer mainIndexer = mainIndex.bulkIndexer();
		final BulkIndexer multiIndex1Indexer = multiIndex1.bulkIndexer();
		final BulkIndexer multiIndex2Indexer = multiIndex2.bulkIndexer();
		final BulkIndexer multiIndex3Indexer = multiIndex3.bulkIndexer();
		final BulkIndexer multiIndex4Indexer = multiIndex4.bulkIndexer();
		initData( mainIndex, mainIndexer,
				multiIndex1, multiIndex1Indexer, multiIndex2, multiIndex2Indexer,
				multiIndex3, multiIndex3Indexer, multiIndex4, multiIndex4Indexer );

		mainIndexer.join( multiIndex1Indexer, multiIndex2Indexer, multiIndex3Indexer, multiIndex4Indexer );
	}

	@Override
	public <R, E, LOS> SearchQueryWhereStep<?, E, LOS, ?> select(SearchQuerySelectStep<?, R, E, LOS, ?, ?> step) {
		return step.select( f -> f.entity() );
	}

	// The entity projection should be usable within an object projection,
	// and in that case it should ignore the object context:
	// it should not try to retrieve fields from the object, but from the root.
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4579")
	public void projectionRegistryFallback_noLoadingAvailable_withProjectionRegistryEntry_inObjectProjection_ignoresObjectContext() {
		DocumentReference doc1Reference = reference( mainIndex.typeName(), DOCUMENT_1_ID );

		ProjectionRegistry projectionRegistryMock = Mockito.mock( ProjectionRegistry.class );
		SearchLoadingContext<StubTransformedReference, StubEntity> loadingContextMock =
				mock( SearchLoadingContext.class );

		when( mainTypeContextMock.loadingAvailable() ).thenReturn( false );
		doReturn( StubEntity.class )
				.when( mainTypeContextMock ).javaClass();
		CompositeProjectionDefinition<StubEntity> projectionDefinitionStub =
				// Simulate a projection that instantiates the entity based on field values extracted from the index.
				// Here we're just retrieving a field containing the ID.
				(f, initialStep) -> initialStep.from( f.field( mainIndex.binding().idField.relativeFieldName, String.class ) )
						.as( id -> new StubEntity( reference( mainIndex.typeName(), id ) ) );
		when( projectionRegistryMock.compositeOptional( StubEntity.class ) )
				.thenReturn( Optional.of( projectionDefinitionStub ) );
		when( projectionRegistryMock.composite( StubEntity.class ) )
				.thenReturn( projectionDefinitionStub );

		mainIndex.mapping().with()
				.typeContext( mainIndex.typeName(), mainTypeContextMock )
				.projectionRegistry( projectionRegistryMock )
				.run( () -> {
					GenericStubMappingScope<StubTransformedReference, StubEntity> scope =
							mainIndex.createGenericScope( loadingContextMock );

					IndexBinding binding = mainIndex.binding();
					SearchQuery<List<List<?>>> query = scope.query( loadingContextMock )
							.select( f -> f.object( binding.nested.absolutePath )
									.from(
											f.field( binding.nested.fieldPath(), String.class ),
											f.entity()
									)
									.asList()
									.multi() )
							.where( f -> f.matchAll() )
							.toQuery();

					@SuppressWarnings("unchecked")
					ProjectionHitMapper<StubTransformedReference, StubEntity> projectionHitMapperMock =
							Mockito.mock( ProjectionHitMapper.class );
					when( loadingContextMock.createProjectionHitMapper() )
							.thenReturn( projectionHitMapperMock );
					assertThatQuery( query )
							.hits().asIs().usingRecursiveFieldByFieldElementComparator()
							.containsExactlyInAnyOrder(
									Arrays.asList(
											// The projection on the root entity will appear once per child object,
											// because that's what was requested.
											Arrays.asList( TEXT_VALUE_1_1, new StubEntity( doc1Reference ) ),
											Arrays.asList( TEXT_VALUE_1_2, new StubEntity( doc1Reference ) )
									),
									Collections.emptyList()
							);
				} );
	}

}
