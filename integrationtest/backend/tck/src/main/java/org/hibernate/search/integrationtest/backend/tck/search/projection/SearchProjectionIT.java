/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubDocumentReferenceTransformer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubEntityLoader;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubLoadedObject;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubTransformedReference;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.GenericStubMappingScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.assertj.core.api.Assertions;
import org.easymock.EasyMockSupport;

/**
 * Generic tests for projections. More specific tests can be found in other classes, such as {@link FieldSearchProjectionIT}.
 */
public class SearchProjectionIT extends EasyMockSupport {

	private static final String INDEX_NAME = "IndexName";
	private static final String ANOTHER_INDEX_NAME = "AnotherIndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY = "empty";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private StubMappingIndexManager anotherIndexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						ANOTHER_INDEX_NAME,
						// Using the same mapping here. But a different mapping would work the same.
						// What matters here is that is a different index.
						ctx -> new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.anotherIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void noProjections() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query = scope.query()
				.asProjections()
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasTotalHitCount( 4 );
	}

	@Test
	public void references() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query;
		DocumentReference document1Reference = reference( INDEX_NAME, DOCUMENT_1 );
		DocumentReference document2Reference = reference( INDEX_NAME, DOCUMENT_2 );
		DocumentReference document3Reference = reference( INDEX_NAME, DOCUMENT_3 );
		DocumentReference emptyReference = reference( INDEX_NAME, EMPTY );

		/*
		 * Note to test writers: make sure to assign these projections to variables,
		 * just so that tests do not compile if someone changes the APIs in an incorrect way.
		 */
		SearchProjection<DocumentReference> documentReferenceProjection =
				scope.projection().documentReference().toProjection();
		SearchProjection<DocumentReference> referenceProjection =
				scope.projection().reference().toProjection();
		SearchProjection<DocumentReference> objectProjection =
				scope.projection().entity().toProjection();

		query = scope.query()
				.asProjections(
						documentReferenceProjection,
						referenceProjection,
						objectProjection
				)
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasListHitsAnyOrder( b -> {
			b.list( document1Reference, document1Reference, document1Reference );
			b.list( document2Reference, document2Reference, document2Reference );
			b.list( document3Reference, document3Reference, document3Reference );
			b.list( emptyReference, emptyReference, emptyReference );
		} );
	}

	/**
	 * Test documentReference/reference/entity projections as they are likely to be used by mappers,
	 * i.e. with a custom reference transformer and a custom entity loader.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3395")
	public void references_transformed() {
		DocumentReference document1Reference = reference( INDEX_NAME, DOCUMENT_1 );
		DocumentReference document2Reference = reference( INDEX_NAME, DOCUMENT_2 );
		DocumentReference document3Reference = reference( INDEX_NAME, DOCUMENT_3 );
		DocumentReference emptyReference = reference( INDEX_NAME, EMPTY );
		StubTransformedReference document1TransformedReference = new StubTransformedReference( document1Reference );
		StubTransformedReference document2TransformedReference = new StubTransformedReference( document2Reference );
		StubTransformedReference document3TransformedReference = new StubTransformedReference( document3Reference );
		StubTransformedReference emptyTransformedReference = new StubTransformedReference( emptyReference );
		StubLoadedObject document1LoadedObject = new StubLoadedObject( document1Reference );
		StubLoadedObject document2LoadedObject = new StubLoadedObject( document2Reference );
		StubLoadedObject document3LoadedObject = new StubLoadedObject( document3Reference );
		StubLoadedObject emptyLoadedObject = new StubLoadedObject( emptyReference );

		LoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock =
				createMock( LoadingContext.class );
		Function<DocumentReference, StubTransformedReference> referenceTransformerMock =
				createMock( StubDocumentReferenceTransformer.class );
		EntityLoader<StubTransformedReference, StubLoadedObject> objectLoaderMock =
				createMock( StubEntityLoader.class );

		resetAll();
		// No call expected on the mocks
		replayAll();
		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope =
				indexManager.createGenericScope();
		SearchQuery<List<?>> query;
		/*
		 * Note to test writers: make sure to assign these projections to variables,
		 * just so that tests do not compile if someone changes the APIs in an incorrect way.
		 */
		SearchProjection<DocumentReference> documentReferenceProjection =
				scope.projection().documentReference().toProjection();
		SearchProjection<StubTransformedReference> referenceProjection =
				scope.projection().reference().toProjection();
		SearchProjection<StubLoadedObject> objectProjection =
				scope.projection().entity().toProjection();
		query = scope.query( loadingContextMock )
				.asProjections(
						documentReferenceProjection,
						referenceProjection,
						objectProjection
				)
				.predicate( f -> f.matchAll() )
				.toQuery();
		verifyAll();

		resetAll();
		StubMapperUtils.expectHitMapping(
				loadingContextMock, referenceTransformerMock, objectLoaderMock,
				/*
				 * Expect each reference to be transformed because of the reference projection,
				 * but also loaded because of the entity projection.
				 */
				c -> c
						.reference( document1Reference, document1TransformedReference )
						.load( document1Reference, document1TransformedReference, document1LoadedObject )
						.reference( document2Reference, document2TransformedReference )
						.load( document2Reference, document2TransformedReference, document2LoadedObject )
						.reference( document3Reference, document3TransformedReference )
						.load( document3Reference, document3TransformedReference, document3LoadedObject )
						.reference( emptyReference, emptyTransformedReference )
						.load( emptyReference, emptyTransformedReference, emptyLoadedObject )
		);
		replayAll();
		assertThat( query ).hasListHitsAnyOrder( b -> {
			b.list( document1Reference, document1TransformedReference, document1LoadedObject );
			b.list( document2Reference, document2TransformedReference, document2LoadedObject );
			b.list( document3Reference, document3TransformedReference, document3LoadedObject );
			b.list( emptyReference, emptyTransformedReference, emptyLoadedObject );
		} );
		verifyAll();
	}

	@Test
	public void score() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Float> query = scope.query()
				.asProjection( f -> f.score() )
				.predicate( f -> f.match().onField( indexMapping.scoreField.relativeFieldName ).matching( "scorepattern" ) )
				.sort( f -> f.byScore().desc() )
				.toQuery();

		SearchResult<Float> result = query.fetch();
		assertThat( result ).hasTotalHitCount( 2 );

		Float score1 = result.getHits().get( 0 );
		Float score2 = result.getHits().get( 1 );

		Assertions.assertThat( score1 ).isNotNull().isNotNaN();
		Assertions.assertThat( score2 ).isNotNull().isNotNaN();

		Assertions.assertThat( score1 ).isGreaterThan( score2 );
	}

	/**
	 * Test projection on the score when we do not sort by score.
	 */
	@Test
	public void score_noScoreSort() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Float> query = scope.query()
				.asProjection( f -> f.score() )
				.predicate( f -> f.match().onField( indexMapping.scoreField.relativeFieldName ).matching( "scorepattern" ) )
				.sort( f -> f.byIndexOrder() )
				.toQuery();

		SearchResult<Float> result = query.fetch();
		assertThat( result ).hasTotalHitCount( 2 );

		Float score1 = result.getHits().get( 0 );
		Float score2 = result.getHits().get( 1 );

		Assertions.assertThat( score1 ).isNotNull().isNotNaN();
		Assertions.assertThat( score2 ).isNotNull().isNotNaN();
	}

	/**
	 * Test mixing multiple projection types (field projections, special projections, ...),
	 * and also multiple field projections.
	 */
	@Test
	public void mixed() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query;

		query = scope.query()
				.asProjection( f ->
						f.composite(
								f.field( indexMapping.string1Field.relativeFieldName, String.class ),
								f.documentReference(),
								f.field( indexMapping.string2Field.relativeFieldName, String.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasListHitsAnyOrder( b -> {
			b.list(
					indexMapping.string1Field.document1Value.indexedValue,
					reference( INDEX_NAME, DOCUMENT_1 ),
					indexMapping.string2Field.document1Value.indexedValue
			);
			b.list(
					indexMapping.string1Field.document2Value.indexedValue,
					reference( INDEX_NAME, DOCUMENT_2 ),
					indexMapping.string2Field.document2Value.indexedValue
			);
			b.list(
					indexMapping.string1Field.document3Value.indexedValue,
					reference( INDEX_NAME, DOCUMENT_3 ),
					indexMapping.string2Field.document3Value.indexedValue
			);
			b.list(
					null,
					reference( INDEX_NAME, EMPTY ),
					null
			);
		} );
	}

	@Test
	public void reuseProjectionInstance_onScopeTargetingSameIndexes() {
		StubMappingScope scope = indexManager.createScope();
		SearchProjection<String> projection = scope.projection()
				.field( indexMapping.string1Field.relativeFieldName, String.class ).toProjection();

		String value1 = indexMapping.string1Field.document1Value.indexedValue;
		String value2 = indexMapping.string1Field.document2Value.indexedValue;
		String value3 = indexMapping.string1Field.document3Value.indexedValue;

		SearchQuery<String> query = scope.query()
				.asProjection( projection )
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder( value1, value2, value3, null );

		// reuse the same projection instance on the same scope
		query = scope.query()
				.asProjection( projection )
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder( value1, value2, value3, null );

		// reuse the same projection instance on a different scope,
		// targeting the same index
		query = indexManager.createScope().query()
				.asProjection( projection )
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder( value1, value2, value3, null );

		projection = indexManager.createScope( anotherIndexManager ).projection()
				.field( indexMapping.string1Field.relativeFieldName, String.class ).toProjection();

		// reuse the same projection instance on a different scope,
		// targeting same indexes
		query = anotherIndexManager.createScope( indexManager ).query()
				.asProjection( projection )
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder( value1, value2, value3, null );
	}

	@Test
	public void reuseProjectionInstance_onScopeTargetingDifferentIndexes() {
		StubMappingScope scope = indexManager.createScope();
		SearchProjection<String> projection = scope.projection()
				.field( indexMapping.string1Field.relativeFieldName, String.class ).toProjection();

		// reuse the same projection instance on a different scope,
		// targeting a different index
		SubTest.expectException( () ->
				anotherIndexManager.createScope().query()
						.asProjection( projection )
						.predicate( f -> f.matchAll() )
						.toQuery() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( INDEX_NAME )
				.hasMessageContaining( ANOTHER_INDEX_NAME );

		// reuse the same projection instance on a different scope,
		// targeting different indexes
		SubTest.expectException( () ->
				indexManager.createScope( anotherIndexManager ).query()
						.asProjection( projection )
						.predicate( f -> f.matchAll() )
						.toQuery() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( INDEX_NAME )
				.hasMessageContaining( ANOTHER_INDEX_NAME );
	}

	@Test
	public void extension() {
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<String> query;

		// Mandatory extension, supported
		query = scope.query()
				.asProjection( f -> f.extension( new SupportedExtension<>() )
						.extendedProjection( "string1", String.class )
				)
				.predicate( f -> f.id().matching( DOCUMENT_1 ) )
				.toQuery();
		assertThat( query )
				.hasHitsAnyOrder( indexMapping.string1Field.document1Value.indexedValue );

		// Mandatory extension, unsupported
		SubTest.expectException(
				() -> scope.projection().extension( new UnSupportedExtension<>() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class );

		// Conditional extensions with orElse - two, both supported
		query = scope.query()
				.asProjection( f -> f.<String>extension()
						.ifSupported(
								new SupportedExtension<>(),
								extended -> extended.extendedProjection( "string1", String.class )
						)
						.ifSupported(
								new SupportedExtension<>(),
								shouldNotBeCalled()
						)
						.orElseFail()
				)
				.predicate( f -> f.id().matching( DOCUMENT_1 ) )
				.toQuery();
		assertThat( query )
				.hasHitsAnyOrder( indexMapping.string1Field.document1Value.indexedValue );

		// Conditional extensions with orElse - two, second supported
		query = scope.query()
				.asProjection( f -> f.<String>extension()
						.ifSupported(
								new UnSupportedExtension<>(),
								shouldNotBeCalled()
						)
						.ifSupported(
								new SupportedExtension<>(),
								extended -> extended.extendedProjection( "string1", String.class )
						)
						.orElse(
								shouldNotBeCalled()
						)
				)
				.predicate( f -> f.id().matching( DOCUMENT_1 ) )
				.toQuery();
		assertThat( query )
				.hasHitsAnyOrder( indexMapping.string1Field.document1Value.indexedValue );

		// Conditional extensions with orElse - two, both unsupported
		query = scope.query()
				.asProjection( f -> f.<String>extension()
						.ifSupported(
								new UnSupportedExtension<>(),
								shouldNotBeCalled()
						)
						.ifSupported(
								new UnSupportedExtension<>(),
								shouldNotBeCalled()
						)
						.orElse(
								c -> c.field( "string1", String.class )
						)
				)
				.predicate( f -> f.id().matching( DOCUMENT_1 ) )
				.toQuery();
		assertThat( query )
				.hasHitsAnyOrder( indexMapping.string1Field.document1Value.indexedValue );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.string1Field.document1Value.write( document );
			indexMapping.string2Field.document1Value.write( document );

			indexMapping.scoreField.document1Value.write( document );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.string1Field.document2Value.write( document );
			indexMapping.string2Field.document2Value.write( document );

			indexMapping.scoreField.document2Value.write( document );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.string1Field.document3Value.write( document );
			indexMapping.string2Field.document3Value.write( document );

			indexMapping.scoreField.document3Value.write( document );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
	}

	private static <T, R> Function<T, R> shouldNotBeCalled() {
		return ignored -> {
			throw new IllegalStateException( "This should not be called" );
		};
	}

	private static class IndexMapping {
		final FieldModel<String> string1Field;
		final FieldModel<String> string2Field;
		final FieldModel<String> scoreField;

		IndexMapping(IndexSchemaElement root) {
			string1Field = FieldModel.mapper( String.class, "ccc", "mmm", "xxx" )
					.map( root, "string1" );
			string2Field = FieldModel.mapper( String.class, "ddd", "nnn", "yyy" )
					.map( root, "string2" );

			scoreField = FieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ),
					"scorepattern scorepattern", "scorepattern", "xxx" )
					.map( root, "score" );
		}
	}

	private static class ValueModel<F> {
		private final IndexFieldReference<F> reference;
		final F indexedValue;

		private ValueModel(IndexFieldReference<F> reference, F indexedValue) {
			this.reference = reference;
			this.indexedValue = indexedValue;
		}

		public void write(DocumentElement target) {
			target.addValue( reference, indexedValue );
		}
	}

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(Class<F> type,
				F document1Value, F document2Value, F document3Value) {
			return mapper(
					c -> (StandardIndexFieldTypeContext<?, F>) c.as( type ),
					document1Value, document2Value, document3Value
			);
		}

		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, F>> configuration,
				F document1Value, F document2Value, F document3Value) {
			return StandardFieldMapper.of(
					configuration,
					c -> c.projectable( Projectable.YES ),
					(reference, name) -> new FieldModel<>(
							reference, name,
							document1Value, document2Value, document3Value
					)
			);
		}

		final String relativeFieldName;

		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;
		final ValueModel<F> document3Value;

		private FieldModel(IndexFieldReference<F> reference, String relativeFieldName,
				F document1Value, F document2Value, F document3Value) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( reference, document1Value );
			this.document2Value = new ValueModel<>( reference, document2Value );
			this.document3Value = new ValueModel<>( reference, document3Value );
		}
	}

	private static class SupportedExtension<R, E>
			implements SearchProjectionFactoryContextExtension<MyExtendedContext<R, E>, R, E> {
		@Override
		public Optional<MyExtendedContext<R, E>> extendOptional(SearchProjectionFactoryContext<R, E> original,
				SearchProjectionBuilderFactory factory) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( factory ).isNotNull();
			return Optional.of( new MyExtendedContext<>( original ) );
		}
	}

	private static class UnSupportedExtension<R, E>
			implements SearchProjectionFactoryContextExtension<MyExtendedContext<R, E>, R, E> {
		@Override
		public Optional<MyExtendedContext<R, E>> extendOptional(SearchProjectionFactoryContext<R, E> original,
				SearchProjectionBuilderFactory factory) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( factory ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedContext<R, E> {
		private final SearchProjectionFactoryContext<R, E> delegate;

		MyExtendedContext(SearchProjectionFactoryContext<R, E> delegate) {
			this.delegate = delegate;
		}

		public <T> SearchProjectionTerminalContext<T> extendedProjection(String fieldName, Class<T> type) {
			return delegate.field( fieldName, type );
		}
	}
}
