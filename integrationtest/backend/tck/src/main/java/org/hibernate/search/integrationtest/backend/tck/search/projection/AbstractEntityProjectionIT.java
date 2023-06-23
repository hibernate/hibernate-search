/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.stub.MapperMockUtils.expectHitMapping;
import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.projection.definition.spi.CompositeProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubEntity;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.GenericStubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingHints;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@SuppressWarnings("unchecked") // Mocking parameterized types
public abstract class AbstractEntityProjectionIT {

	protected static final String DOCUMENT_1_ID = "1";
	protected static final String DOCUMENT_2_ID = "2";
	protected static final String TEXT_VALUE_1_1 = "some text 1_1";
	protected static final String TEXT_VALUE_1_2 = "some text 1_2";

	protected static final ProjectionMappedTypeContext mainTypeContextMock = Mockito.mock( ProjectionMappedTypeContext.class );

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private final SimpleMappedIndex<IndexBinding> mainIndex;
	private final SimpleMappedIndex<IndexBinding> multiIndex1;
	private final SimpleMappedIndex<IndexBinding> multiIndex2;
	private final SimpleMappedIndex<IndexBinding> multiIndex3;
	private final SimpleMappedIndex<IndexBinding> multiIndex4;

	protected AbstractEntityProjectionIT(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<IndexBinding> multiIndex1, SimpleMappedIndex<IndexBinding> multiIndex2,
			SimpleMappedIndex<IndexBinding> multiIndex3, SimpleMappedIndex<IndexBinding> multiIndex4) {
		this.mainIndex = mainIndex;
		this.multiIndex1 = multiIndex1;
		this.multiIndex2 = multiIndex2;
		this.multiIndex3 = multiIndex3;
		this.multiIndex4 = multiIndex4;
	}

	public abstract <R, E, LOS> SearchQueryWhereStep<?, E, LOS, ?> select(
			SearchQuerySelectStep<?, R, E, LOS, ?, ?> step);

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3578")
	public void entityLoading() {
		DocumentReference doc1Reference = reference( mainIndex.typeName(), DOCUMENT_1_ID );
		DocumentReference doc2Reference = reference( mainIndex.typeName(), DOCUMENT_2_ID );
		StubEntity doc1LoadedEntity = new StubEntity( doc1Reference );
		StubEntity doc2LoadedEntity = new StubEntity( doc2Reference );

		SearchLoadingContext<StubEntity> loadingContextMock =
				mock( SearchLoadingContext.class );

		when( mainTypeContextMock.loadingAvailable() ).thenReturn( true );

		mainIndex.mapping().with()
				.typeContext( mainIndex.typeName(), mainTypeContextMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope =
							mainIndex.createGenericScope( loadingContextMock );
					SearchQuery<StubEntity> query = select( scope.query() )
							.where( f -> f.matchAll() )
							.toQuery();

					expectHitMapping(
							loadingContextMock,
							c -> c
									.load( doc1Reference, doc1LoadedEntity )
									.load( doc2Reference, doc2LoadedEntity )
					);
					assertThatQuery( query ).hasHitsAnyOrder( doc1LoadedEntity, doc2LoadedEntity );
					// Check in particular that the backend gets the projection hit mapper from the loading context,
					// which must happen every time we execute the query,
					// so that the mapper can run state checks (session is still open, ...).
					verify( loadingContextMock ).createProjectionHitMapper();

					// check the same for the scroll API
					expectHitMapping(
							loadingContextMock,
							c -> c
									.load( doc1Reference, doc1LoadedEntity )
									.load( doc2Reference, doc2LoadedEntity )
					);
					assertThatHits( hitsUsingScroll( query ) ).hasHitsAnyOrder( doc1LoadedEntity, doc2LoadedEntity );
					verify( loadingContextMock ).createProjectionHitMapper();
				} );
	}

	@Test
	public void entityLoading_timeout() {
		DocumentReference doc1Reference = reference( mainIndex.typeName(), DOCUMENT_1_ID );
		DocumentReference doc2Reference = reference( mainIndex.typeName(), DOCUMENT_2_ID );
		StubEntity doc1LoadedEntity = new StubEntity( doc1Reference );
		StubEntity doc2LoadedEntity = new StubEntity( doc2Reference );

		SearchLoadingContext<StubEntity> loadingContextMock =
				mock( SearchLoadingContext.class );

		when( mainTypeContextMock.loadingAvailable() ).thenReturn( true );

		mainIndex.mapping().with()
				.typeContext( mainIndex.typeName(), mainTypeContextMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope =
							mainIndex.createGenericScope( loadingContextMock );
					SearchQuery<StubEntity> query = select( scope.query() )
							.where( f -> f.matchAll() )
							.failAfter( 1000L, TimeUnit.HOURS )
							.toQuery();

					expectHitMapping(
							loadingContextMock,
							c -> c
									.load( doc1Reference, doc1LoadedEntity )
									.load( doc2Reference, doc2LoadedEntity )
					);
					assertThatQuery( query ).hasHitsAnyOrder( doc1LoadedEntity, doc2LoadedEntity );
					// Check in particular that the backend gets the projection hit mapper from the loading context,
					// which must happen every time we execute the query,
					// so that the mapper can run state checks (session is still open, ...).
					verify( loadingContextMock ).createProjectionHitMapper();

					// check the same for the scroll API
					expectHitMapping(
							loadingContextMock,
							c -> c
									.load( doc1Reference, doc1LoadedEntity )
									.load( doc2Reference, doc2LoadedEntity )
					);
					assertThatHits( hitsUsingScroll( query ) ).hasHitsAnyOrder( doc1LoadedEntity, doc2LoadedEntity );
					verify( loadingContextMock ).createProjectionHitMapper();
				} );
	}

	@Test
	public void noEntityLoading() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = select( scope.query() )
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1_ID, DOCUMENT_2_ID );

		// check the same for the scroll API
		assertThatHits( hitsUsingScroll( query ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1_ID, DOCUMENT_2_ID );
	}

	@Test
	public void entityLoading_callGetProjectionHitMapperEveryTime() {
		DocumentReference doc1Reference = reference( mainIndex.typeName(), DOCUMENT_1_ID );
		DocumentReference doc2Reference = reference( mainIndex.typeName(), DOCUMENT_2_ID );

		SearchLoadingContext<DocumentReference> loadingContextMock =
				mock( SearchLoadingContext.class );

		when( mainTypeContextMock.loadingAvailable() ).thenReturn( true );

		mainIndex.mapping().with()
				.typeContext( mainIndex.typeName(), mainTypeContextMock )
				.run( () -> {
					GenericStubMappingScope<DocumentReference, DocumentReference> scope =
							mainIndex.createGenericScope( loadingContextMock );
					SearchQuery<DocumentReference> query = select( scope.query() )
							.where( f -> f.matchAll() )
							.toQuery();

					expectHitMapping(
							loadingContextMock,
							c -> c
									.load( doc1Reference, doc1Reference )
									.load( doc2Reference, doc2Reference )
					);
					query.fetchAll();
					// Check in particular that the backend gets the projection hit mapper from the loading context,
					// which must happen every time we execute the query,
					// so that the mapper can run state checks (session is still open, ...).
					verify( loadingContextMock ).createProjectionHitMapper();

					// Second query execution to make sure the backend doesn't try to cache the projection hit mapper...
					reset( loadingContextMock );
					expectHitMapping(
							loadingContextMock,
							c -> c
									.load( doc1Reference, doc1Reference )
									.load( doc2Reference, doc2Reference )
					);
					query.fetchAll();
					verify( loadingContextMock ).createProjectionHitMapper();
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void entityLoading_failed_skipHit() {
		DocumentReference doc1Reference = reference( mainIndex.typeName(), DOCUMENT_1_ID );
		DocumentReference doc2Reference = reference( mainIndex.typeName(), DOCUMENT_2_ID );
		StubEntity doc2LoadedObject = new StubEntity( doc2Reference );

		SearchLoadingContext<StubEntity> loadingContextMock =
				mock( SearchLoadingContext.class );

		when( mainTypeContextMock.loadingAvailable() ).thenReturn( true );

		mainIndex.mapping().with()
				.typeContext( mainIndex.typeName(), mainTypeContextMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope =
							mainIndex.createGenericScope( loadingContextMock );
					SearchQuery<StubEntity> query = select( scope.query() )
							.where( f -> f.matchAll() )
							.toQuery();

					expectHitMapping(
							loadingContextMock,
							c -> c
									// Return "null" when loading, meaning the entity failed to load
									.load( doc1Reference, null )
									.load( doc2Reference, doc2LoadedObject )
					);
					// Expect the main document to be excluded from hits, since it could not be loaded.
					assertThatQuery( query ).hasHitsAnyOrder( doc2LoadedObject );
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4579")
	public void noLoadingAvailable_noProjectionRegistryEntry_fails() {
		ProjectionRegistry projectionRegistryMock = Mockito.mock( ProjectionRegistry.class );

		SearchLoadingContext<DocumentReference> loadingContextMock =
				mock( SearchLoadingContext.class );

		when( mainTypeContextMock.loadingAvailable() ).thenReturn( false );
		when( mainTypeContextMock.name() ).thenReturn( mainIndex.typeName() );
		doReturn( StubEntity.class )
				.when( mainTypeContextMock ).javaClass();
		when( projectionRegistryMock.compositeOptional( StubEntity.class ) )
				.thenReturn( Optional.empty() );

		mainIndex.mapping().with()
				.typeContext( mainIndex.typeName(), mainTypeContextMock )
				.projectionRegistry( projectionRegistryMock )
				.run( () -> {
					GenericStubMappingScope<DocumentReference, DocumentReference> scope =
							mainIndex.createGenericScope( loadingContextMock );
					SearchQuery<DocumentReference> query = select( scope.query( loadingContextMock ) )
							.where( f -> f.matchAll() )
							.toQuery();

					assertThatThrownBy( query::fetchAll )
							.isInstanceOf( SearchException.class )
							.hasMessageContainingAll(
									"Cannot project on entity type '" + mainIndex.typeName()
											+ "': this type cannot be loaded from an external datasource,"
											+ " and the documents from the index cannot be projected to its Java class '"
											+ StubEntity.class.getName() + "'",
									StubMappingHints.INSTANCE.noEntityProjectionAvailable()
							);

					// Fetching the total hit count should still work fine,
					// since the projection is not needed in that case.
					// This is especially important for the syntax .search(MyEntity.class).where(...).fetchTotalHitCount(),
					// where the (entity) projection is impossible but it's just the default so it's not the user's fault,
					// and in the end does not matter because the projection is never executed anyway.
					assertThat( query.fetchTotalHitCount() ).isEqualTo( 2 );
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4579")
	public void projectionRegistryFallback_withLoadingAvailable_doesNotCheckProjectionRegistry() {
		DocumentReference doc1Reference = reference( mainIndex.typeName(), DOCUMENT_1_ID );
		DocumentReference doc2Reference = reference( mainIndex.typeName(), DOCUMENT_2_ID );
		StubEntity doc1LoadedEntity = new StubEntity( doc1Reference );
		StubEntity doc2LoadedEntity = new StubEntity( doc2Reference );

		ProjectionRegistry projectionRegistryMock = Mockito.mock( ProjectionRegistry.class );
		SearchLoadingContext<StubEntity> loadingContextMock =
				mock( SearchLoadingContext.class );

		when( mainTypeContextMock.loadingAvailable() ).thenReturn( true );

		mainIndex.mapping().with()
				.typeContext( mainIndex.typeName(), mainTypeContextMock )
				.projectionRegistry( projectionRegistryMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope =
							mainIndex.createGenericScope( loadingContextMock );

					expectHitMapping(
							loadingContextMock,
							c -> c
									.load( doc1Reference, doc1LoadedEntity )
									.load( doc2Reference, doc2LoadedEntity )
					);
					SearchQuery<StubEntity> query = select( scope.query( loadingContextMock ) )
							.where( f -> f.matchAll() )
							.toQuery();

					assertThatQuery( query ).hasHitsAnyOrder( doc1LoadedEntity, doc2LoadedEntity );
				} );

		// We don't want the projection registry to be checked at any point:
		// loading is available, so the projection registry is irrelevant.
		verify( projectionRegistryMock, never() ).compositeOptional( any() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4579")
	public void projectionRegistryFallback_noLoadingAvailable_withProjectionRegistryEntry_usesProjectionRegistry() {
		DocumentReference doc1Reference = reference( mainIndex.typeName(), DOCUMENT_1_ID );
		DocumentReference doc2Reference = reference( mainIndex.typeName(), DOCUMENT_2_ID );

		ProjectionRegistry projectionRegistryMock = Mockito.mock( ProjectionRegistry.class );
		SearchLoadingContext<StubEntity> loadingContextMock =
				mock( SearchLoadingContext.class );

		when( mainTypeContextMock.loadingAvailable() ).thenReturn( false );
		doReturn( StubEntity.class )
				.when( mainTypeContextMock ).javaClass();
		CompositeProjectionDefinition<StubEntity> projectionDefinitionStub =
				// Simulate a projection that instantiates the entity based on field values extracted from the index.
				// Here we're just retrieving a field containing the ID.
				(f, initialStep, ctx) -> initialStep
						.from( f.field( mainIndex.binding().idField.relativeFieldName, String.class ) )
						.as( id -> new StubEntity( reference( mainIndex.typeName(), id ) ) );
		when( projectionRegistryMock.compositeOptional( StubEntity.class ) )
				.thenReturn( Optional.of( projectionDefinitionStub ) );
		when( projectionRegistryMock.composite( StubEntity.class ) )
				.thenReturn( projectionDefinitionStub );

		mainIndex.mapping().with()
				.typeContext( mainIndex.typeName(), mainTypeContextMock )
				.projectionRegistry( projectionRegistryMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope =
							mainIndex.createGenericScope( loadingContextMock );

					SearchQuery<StubEntity> query = select( scope.query( loadingContextMock ) )
							.where( f -> f.matchAll() )
							.toQuery();

					@SuppressWarnings("unchecked")
					ProjectionHitMapper<StubEntity> projectionHitMapperMock =
							Mockito.mock( ProjectionHitMapper.class );
					when( loadingContextMock.createProjectionHitMapper() )
							.thenReturn( projectionHitMapperMock );
					assertThatQuery( query )
							.hits().asIs().usingRecursiveFieldByFieldElementComparator()
							.containsExactlyInAnyOrder(
									new StubEntity( doc1Reference ),
									new StubEntity( doc2Reference )
							);
				} );
	}

	/**
	 * Tests the entity projection when targeting multiple types:
	 * two types with loading available,
	 * and two types with loading unavailable but each with its own projection in the registry.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4579")
	public void projectionRegistryFallback_multiType() {
		DocumentReference type1Doc1Reference = reference( multiIndex1.typeName(), DOCUMENT_1_ID );
		DocumentReference type1Doc2Reference = reference( multiIndex1.typeName(), DOCUMENT_2_ID );
		DocumentReference type2Doc1Reference = reference( multiIndex2.typeName(), DOCUMENT_1_ID );
		DocumentReference type2Doc2Reference = reference( multiIndex2.typeName(), DOCUMENT_2_ID );
		DocumentReference type3Doc1Reference = reference( multiIndex3.typeName(), DOCUMENT_1_ID );
		DocumentReference type3Doc2Reference = reference( multiIndex3.typeName(), DOCUMENT_2_ID );
		DocumentReference type4Doc1Reference = reference( multiIndex4.typeName(), DOCUMENT_1_ID );
		DocumentReference type4Doc2Reference = reference( multiIndex4.typeName(), DOCUMENT_2_ID );

		ProjectionMappedTypeContext type1ContextMock = Mockito.mock( ProjectionMappedTypeContext.class );
		ProjectionMappedTypeContext type2ContextMock = Mockito.mock( ProjectionMappedTypeContext.class );
		ProjectionMappedTypeContext type3ContextMock = Mockito.mock( ProjectionMappedTypeContext.class );
		ProjectionMappedTypeContext type4ContextMock = Mockito.mock( ProjectionMappedTypeContext.class );
		ProjectionRegistry projectionRegistryMock = Mockito.mock( ProjectionRegistry.class );
		SearchLoadingContext<StubEntity> loadingContextMock =
				mock( SearchLoadingContext.class );

		when( type1ContextMock.name() ).thenReturn( multiIndex1.typeName() );
		when( type1ContextMock.loadingAvailable() ).thenReturn( true );

		when( type2ContextMock.name() ).thenReturn( multiIndex2.typeName() );
		when( type2ContextMock.loadingAvailable() ).thenReturn( true );

		when( type3ContextMock.name() ).thenReturn( multiIndex3.typeName() );
		when( type3ContextMock.loadingAvailable() ).thenReturn( false );
		doReturn( StubType3.class )
				.when( type3ContextMock ).javaClass();
		CompositeProjectionDefinition<StubType3> type3ProjectionDefinitionStub =
				// Simulate a projection that instantiates the entity based on field values extracted from the index.
				// Here we're just retrieving a field containing the ID.
				(f, initialStep, ctx) -> initialStep
						.from( f.field( multiIndex3.binding().idField.relativeFieldName, String.class ) )
						.as( id -> new StubType3( reference( multiIndex3.typeName(), id ) ) );
		when( projectionRegistryMock.compositeOptional( StubType3.class ) )
				.thenReturn( Optional.of( type3ProjectionDefinitionStub ) );
		when( projectionRegistryMock.composite( StubType3.class ) )
				.thenReturn( type3ProjectionDefinitionStub );

		when( type4ContextMock.name() ).thenReturn( multiIndex4.typeName() );
		when( type4ContextMock.loadingAvailable() ).thenReturn( false );
		doReturn( StubType4.class )
				.when( type4ContextMock ).javaClass();
		CompositeProjectionDefinition<StubType4> type4ProjectionDefinitionStub =
				// Simulate a projection that instantiates the entity based on field values extracted from the index.
				// Here we're just retrieving a field containing the ID.
				(f, initialStep, ctx) -> initialStep
						.from( f.field( multiIndex4.binding().idField.relativeFieldName, String.class ) )
						.as( id -> new StubType4( reference( multiIndex4.typeName(), id ) ) );
		when( projectionRegistryMock.compositeOptional( StubType4.class ) )
				.thenReturn( Optional.of( type4ProjectionDefinitionStub ) );
		when( projectionRegistryMock.composite( StubType4.class ) )
				.thenReturn( type4ProjectionDefinitionStub );

		multiIndex1.mapping().with()
				.typeContext( multiIndex1.typeName(), type1ContextMock )
				.typeContext( multiIndex2.typeName(), type2ContextMock )
				.typeContext( multiIndex3.typeName(), type3ContextMock )
				.typeContext( multiIndex4.typeName(), type4ContextMock )
				.projectionRegistry( projectionRegistryMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope =
							multiIndex1.createGenericScope( loadingContextMock, multiIndex2, multiIndex3, multiIndex4 );

					SearchQuery<StubEntity> query = select( scope.query( loadingContextMock ) )
							.where( f -> f.matchAll() )
							.toQuery();

					expectHitMapping(
							loadingContextMock,
							c -> c
									.load( type1Doc1Reference, new StubType1( type1Doc1Reference ) )
									.load( type1Doc2Reference, new StubType1( type1Doc2Reference ) )
									.load( type2Doc1Reference, new StubType2( type2Doc1Reference ) )
									.load( type2Doc2Reference, new StubType2( type2Doc2Reference ) )
					);
					assertThatQuery( query )
							.hits().asIs().usingRecursiveFieldByFieldElementComparator()
							.containsExactlyInAnyOrder(
									new StubType1( type1Doc1Reference ),
									new StubType1( type1Doc2Reference ),
									new StubType2( type2Doc1Reference ),
									new StubType2( type2Doc2Reference ),
									new StubType3( type3Doc1Reference ),
									new StubType3( type3Doc2Reference ),
									new StubType4( type4Doc1Reference ),
									new StubType4( type4Doc2Reference )
							);
				} );
	}

	private static <H> List<H> hitsUsingScroll(SearchQuery<H> query) {
		try ( SearchScroll<H> scroll = query.scroll( 10 ) ) {
			return scroll.next().hits();
		}
	}

	public static void initData(SimpleMappedIndex<IndexBinding> mainIndex, BulkIndexer mainIndexer,
			SimpleMappedIndex<IndexBinding> multiIndex1, BulkIndexer multiIndex1Indexer,
			SimpleMappedIndex<IndexBinding> multiIndex2, BulkIndexer multiIndex2Indexer,
			SimpleMappedIndex<IndexBinding> multiIndex3, BulkIndexer multiIndex3Indexer,
			SimpleMappedIndex<IndexBinding> multiIndex4, BulkIndexer multiIndex4Indexer) {
		initIndex( mainIndex, mainIndexer );
		initIndex( multiIndex1, multiIndex1Indexer );
		initIndex( multiIndex2, multiIndex2Indexer );
		initIndex( multiIndex3, multiIndex3Indexer );
		initIndex( multiIndex4, multiIndex4Indexer );
	}

	private static void initIndex(SimpleMappedIndex<IndexBinding> index, BulkIndexer indexer) {
		IndexBinding binding = index.binding();
		indexer
				.add( DOCUMENT_1_ID, document -> {
					document.addValue( binding.idField.reference, DOCUMENT_1_ID );
					DocumentElement object = document.addObject( binding.nested.reference );
					object.addValue( binding.nested.field.reference, TEXT_VALUE_1_1 );
					object = document.addObject( binding.nested.reference );
					object.addValue( binding.nested.field.reference, TEXT_VALUE_1_2 );
				} )
				.add( DOCUMENT_2_ID, document -> document.addValue( binding.idField.reference, DOCUMENT_2_ID ) );
	}


	public static class IndexBinding {
		final SimpleFieldModel<String> idField;
		final ObjectFieldBinding nested;

		public IndexBinding(IndexSchemaElement root) {
			idField = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "id", c -> c.projectable(
							TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault()
									? Projectable.DEFAULT
									: Projectable.YES ) );
			nested = ObjectFieldBinding.create( root, "nested" );
		}
	}

	static class ObjectFieldBinding {
		final IndexObjectFieldReference reference;
		final String absolutePath;

		final SimpleFieldModel<String> field;

		static ObjectFieldBinding create(IndexSchemaElement parent, String relativeFieldName) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, ObjectStructure.NESTED )
					.multiValued();
			return new ObjectFieldBinding( objectField, relativeFieldName );
		}

		ObjectFieldBinding(IndexSchemaObjectField objectField, String absolutePath) {
			reference = objectField.toReference();
			this.absolutePath = absolutePath;
			field = SimpleFieldModel.mapper( AnalyzedStringFieldTypeDescriptor.INSTANCE )
					.map( objectField, "field", c -> c.projectable(
							TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault()
									? Projectable.DEFAULT
									: Projectable.YES ) );
		}

		String fieldPath() {
			return absolutePath + "." + field.relativeFieldName;
		}
	}

	static final class StubType1 extends StubEntity {
		public StubType1(DocumentReference documentReference) {
			super( documentReference );
		}
	}

	static final class StubType2 extends StubEntity {
		public StubType2(DocumentReference documentReference) {
			super( documentReference );
		}
	}

	static final class StubType3 extends StubEntity {
		public StubType3(DocumentReference documentReference) {
			super( documentReference );
		}
	}

	static final class StubType4 extends StubEntity {
		public StubType4(DocumentReference documentReference) {
			super( documentReference );
		}
	}
}
