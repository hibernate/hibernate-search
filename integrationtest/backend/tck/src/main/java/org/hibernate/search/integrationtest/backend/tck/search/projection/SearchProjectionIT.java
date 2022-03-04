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
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactoryExtension;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubDocumentReferenceConverter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubLoadedObject;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubTransformedReference;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.GenericStubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * Generic tests for projections. More specific tests can be found in other classes, such as {@link FieldProjectionSingleValuedBaseIT}.
 */
@SuppressWarnings("unchecked") // Mocking parameterized types
public class SearchProjectionIT {

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY = "empty";

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final SimpleMappedIndex<IndexBinding> otherIndex =
			// Using the same mapping here. But a different mapping would work the same.
			// What matters here is that is a different index.
			SimpleMappedIndex.of( IndexBinding::new ).name( "other" );

	@Before
	public void setup() {
		setupHelper.start().withIndexes( mainIndex, otherIndex ).setup();

		initData();
	}

	@Test
	public void noProjections() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<List<?>> query = scope.query()
				.select()
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasTotalHitCount( 4 );
	}

	@Test
	public void references() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<List<?>> query;
		DocumentReference document1Reference = reference( mainIndex.typeName(), DOCUMENT_1 );
		DocumentReference document2Reference = reference( mainIndex.typeName(), DOCUMENT_2 );
		DocumentReference document3Reference = reference( mainIndex.typeName(), DOCUMENT_3 );
		DocumentReference emptyReference = reference( mainIndex.typeName(), EMPTY );

		/*
		 * Note to test writers: make sure to assign these projections to variables,
		 * just so that tests do not compile if someone changes the APIs in an incorrect way.
		 */
		SearchProjection<DocumentReference> documentReferenceProjection =
				scope.projection().documentReference().toProjection();
		SearchProjection<DocumentReference> entityReferenceProjection =
				scope.projection().entityReference().toProjection();
		SearchProjection<DocumentReference> objectProjection =
				scope.projection().entity().toProjection();
		SearchProjection<Object> idProjection =
				scope.projection().id().toProjection();

		query = scope.query()
				.select(
						documentReferenceProjection,
						entityReferenceProjection,
						objectProjection,
						idProjection
				)
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> {
			b.list( document1Reference, document1Reference, document1Reference, DOCUMENT_1 );
			b.list( document2Reference, document2Reference, document2Reference, DOCUMENT_2 );
			b.list( document3Reference, document3Reference, document3Reference, DOCUMENT_3 );
			b.list( emptyReference, emptyReference, emptyReference, EMPTY );
		} );
	}

	/**
	 * Test documentReference/entityReference/entity projections as they are likely to be used by mappers,
	 * i.e. with a custom reference transformer and a custom entity loader.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3395")
	public void references_transformed() {
		DocumentReference document1Reference = reference( mainIndex.typeName(), DOCUMENT_1 );
		DocumentReference document2Reference = reference( mainIndex.typeName(), DOCUMENT_2 );
		DocumentReference document3Reference = reference( mainIndex.typeName(), DOCUMENT_3 );
		DocumentReference emptyReference = reference( mainIndex.typeName(), EMPTY );
		StubTransformedReference document1TransformedReference = new StubTransformedReference( document1Reference );
		StubTransformedReference document2TransformedReference = new StubTransformedReference( document2Reference );
		StubTransformedReference document3TransformedReference = new StubTransformedReference( document3Reference );
		StubTransformedReference emptyTransformedReference = new StubTransformedReference( emptyReference );
		StubLoadedObject document1LoadedObject = new StubLoadedObject( document1Reference );
		StubLoadedObject document2LoadedObject = new StubLoadedObject( document2Reference );
		StubLoadedObject document3LoadedObject = new StubLoadedObject( document3Reference );
		StubLoadedObject emptyLoadedObject = new StubLoadedObject( emptyReference );

		SearchLoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock =
				mock( SearchLoadingContext.class );
		DocumentReferenceConverter<StubTransformedReference> documentReferenceConverterMock =
				mock( StubDocumentReferenceConverter.class );

		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope =
				mainIndex.createGenericScope();
		SearchQuery<List<?>> query;
		/*
		 * Note to test writers: make sure to assign these projections to variables,
		 * just so that tests do not compile if someone changes the APIs in an incorrect way.
		 */
		SearchProjection<DocumentReference> documentReferenceProjection =
				scope.projection().documentReference().toProjection();
		SearchProjection<StubTransformedReference> entityReferenceProjection =
				scope.projection().entityReference().toProjection();
		SearchProjection<StubLoadedObject> objectProjection =
				scope.projection().entity().toProjection();
		query = scope.query( loadingContextMock )
				.select(
						documentReferenceProjection,
						entityReferenceProjection,
						objectProjection
				)
				.where( f -> f.matchAll() )
				.toQuery();

		expectHitMapping(
				loadingContextMock, documentReferenceConverterMock,
				/*
				 * Expect each reference to be transformed because of the reference projection,
				 * but also loaded because of the entity projection.
				 */
				c -> c
						.entityReference( document1Reference, document1TransformedReference )
						.load( document1Reference, document1LoadedObject )
						.entityReference( document2Reference, document2TransformedReference )
						.load( document2Reference, document2LoadedObject )
						.entityReference( document3Reference, document3TransformedReference )
						.load( document3Reference, document3LoadedObject )
						.entityReference( emptyReference, emptyTransformedReference )
						.load( emptyReference, emptyLoadedObject )
		);
		assertThatQuery( query ).hasListHitsAnyOrder( b -> {
			b.list( document1Reference, document1TransformedReference, document1LoadedObject );
			b.list( document2Reference, document2TransformedReference, document2LoadedObject );
			b.list( document3Reference, document3TransformedReference, document3LoadedObject );
			b.list( emptyReference, emptyTransformedReference, emptyLoadedObject );
		} );
	}

	@Test
	public void score() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<Float> query = scope.query()
				.select( f -> f.score() )
				.where( f -> f.match().field( mainIndex.binding().scoreField.relativeFieldName ).matching( "scorepattern" ) )
				.sort( f -> f.score().desc() )
				.toQuery();

		SearchResult<Float> result = query.fetchAll();
		assertThatResult( result ).hasTotalHitCount( 2 );

		Float score1 = result.hits().get( 0 );
		Float score2 = result.hits().get( 1 );

		assertThat( score1 ).isNotNull().isNotNaN();
		assertThat( score2 ).isNotNull().isNotNaN();

		assertThat( score1 ).isGreaterThan( score2 );
	}

	/**
	 * Test projection on the score when we do not sort by score.
	 */
	@Test
	public void score_noScoreSort() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<Float> query = scope.query()
				.select( f -> f.score() )
				.where( f -> f.match().field( mainIndex.binding().scoreField.relativeFieldName ).matching( "scorepattern" ) )
				.sort( f -> f.indexOrder() )
				.toQuery();

		SearchResult<Float> result = query.fetchAll();
		assertThatResult( result ).hasTotalHitCount( 2 );

		Float score1 = result.hits().get( 0 );
		Float score2 = result.hits().get( 1 );

		assertThat( score1 ).isNotNull().isNotNaN();
		assertThat( score2 ).isNotNull().isNotNaN();
	}

	/**
	 * Test mixing multiple projection types (field projections, special projections, ...),
	 * and also multiple field projections.
	 */
	@Test
	public void mixed() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<List<?>> query;

		query = scope.query()
				.select( f ->
						f.composite(
								f.field( mainIndex.binding().string1Field.relativeFieldName, String.class ),
								f.documentReference(),
								f.field( mainIndex.binding().string2Field.relativeFieldName, String.class )
						)
				)
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> {
			b.list(
					mainIndex.binding().string1Field.document1Value.indexedValue,
					reference( mainIndex.typeName(), DOCUMENT_1 ),
					mainIndex.binding().string2Field.document1Value.indexedValue
			);
			b.list(
					mainIndex.binding().string1Field.document2Value.indexedValue,
					reference( mainIndex.typeName(), DOCUMENT_2 ),
					mainIndex.binding().string2Field.document2Value.indexedValue
			);
			b.list(
					mainIndex.binding().string1Field.document3Value.indexedValue,
					reference( mainIndex.typeName(), DOCUMENT_3 ),
					mainIndex.binding().string2Field.document3Value.indexedValue
			);
			b.list(
					null,
					reference( mainIndex.typeName(), EMPTY ),
					null
			);
		} );
	}

	/**
	 * Test mixing multiple projection types (field projections, special projections, ...),
	 * and also multiple field projections, using nested fields too.
	 */
	@Test
	public void mixed_withNestedFields() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<List<?>> query;

		query = scope.query()
				.select( f ->
						f.composite(
								f.field( mainIndex.binding().string1Field.relativeFieldName, String.class ),
								f.documentReference(),
								f.field( "nested." + mainIndex.binding().nestedField.relativeFieldName, String.class ),
								f.field( "nested.nested." + mainIndex.binding().nestedNestedField.relativeFieldName, String.class ),
								f.field( "nested.flattened." + mainIndex.binding().flattenedField.relativeFieldName, String.class )
						)
				)
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> {
			b.list(
					mainIndex.binding().string1Field.document1Value.indexedValue,
					reference( mainIndex.typeName(), DOCUMENT_1 ),
					mainIndex.binding().nestedField.document1Value.indexedValue,
					mainIndex.binding().nestedNestedField.document1Value.indexedValue,
					mainIndex.binding().flattenedField.document1Value.indexedValue
			);
			b.list(
					mainIndex.binding().string1Field.document2Value.indexedValue,
					reference( mainIndex.typeName(), DOCUMENT_2 ),
					mainIndex.binding().nestedField.document2Value.indexedValue,
					mainIndex.binding().nestedNestedField.document2Value.indexedValue,
					mainIndex.binding().flattenedField.document2Value.indexedValue
			);
			b.list(
					mainIndex.binding().string1Field.document3Value.indexedValue,
					reference( mainIndex.typeName(), DOCUMENT_3 ),
					mainIndex.binding().nestedField.document3Value.indexedValue,
					mainIndex.binding().nestedNestedField.document3Value.indexedValue,
					mainIndex.binding().flattenedField.document3Value.indexedValue
			);
			b.list(
					null,
					reference( mainIndex.typeName(), EMPTY ),
					null,
					null,
					null
			);
		} );
	}

	@Test
	public void reuseProjectionInstance_onScopeTargetingSameIndexes() {
		StubMappingScope scope = mainIndex.createScope();
		SearchProjection<String> projection = scope.projection()
				.field( mainIndex.binding().string1Field.relativeFieldName, String.class ).toProjection();

		String value1 = mainIndex.binding().string1Field.document1Value.indexedValue;
		String value2 = mainIndex.binding().string1Field.document2Value.indexedValue;
		String value3 = mainIndex.binding().string1Field.document3Value.indexedValue;

		SearchQuery<String> query = scope.query()
				.select( projection )
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( value1, value2, value3, null );

		// reuse the same projection instance on the same scope
		query = scope.query()
				.select( projection )
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( value1, value2, value3, null );

		// reuse the same projection instance on a different scope,
		// targeting the same index
		query = mainIndex.createScope().query()
				.select( projection )
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( value1, value2, value3, null );

		projection = mainIndex.createScope( otherIndex ).projection()
				.field( mainIndex.binding().string1Field.relativeFieldName, String.class ).toProjection();

		// reuse the same projection instance on a different scope,
		// targeting same indexes
		query = otherIndex.createScope( mainIndex ).query()
				.select( projection )
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( value1, value2, value3, null );
	}

	@Test
	public void reuseProjectionInstance_onScopeTargetingDifferentIndexes() {
		StubMappingScope scope = mainIndex.createScope();
		SearchProjection<String> projection = scope.projection()
				.field( mainIndex.binding().string1Field.relativeFieldName, String.class ).toProjection();

		// reuse the same projection instance on a different scope,
		// targeting a different index
		assertThatThrownBy( () ->
				otherIndex.createScope().query()
						.select( projection )
						.where( f -> f.matchAll() )
						.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search projection",
						"You must build the projection from a scope targeting indexes ", otherIndex.name(),
						"the given projection was built from a scope targeting ", mainIndex.name() );

		// reuse the same projection instance on a different scope,
		// targeting different indexes
		assertThatThrownBy( () ->
				mainIndex.createScope( otherIndex ).query()
						.select( projection )
						.where( f -> f.matchAll() )
						.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search projection",
						"You must build the projection from a scope targeting indexes ",
						mainIndex.name(), otherIndex.name(),
						"the given projection was built from a scope targeting ", mainIndex.name() );
	}

	@Test
	public void extension() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<String> query;

		// Mandatory extension, supported
		query = scope.query()
				.select( f -> f.extension( new SupportedExtension<>() )
						.extendedProjection( "string1", String.class )
				)
				.where( f -> f.id().matching( DOCUMENT_1 ) )
				.toQuery();
		assertThatQuery( query )
				.hasHitsAnyOrder( mainIndex.binding().string1Field.document1Value.indexedValue );

		// Mandatory extension, unsupported
		assertThatThrownBy(
				() -> scope.projection().extension( new UnSupportedExtension<>() )
		)
				.isInstanceOf( SearchException.class );

		// Conditional extensions with orElse - two, both supported
		query = scope.query()
				.select( f -> f.<String>extension()
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
				.where( f -> f.id().matching( DOCUMENT_1 ) )
				.toQuery();
		assertThatQuery( query )
				.hasHitsAnyOrder( mainIndex.binding().string1Field.document1Value.indexedValue );

		// Conditional extensions with orElse - two, second supported
		query = scope.query()
				.select( f -> f.<String>extension()
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
				.where( f -> f.id().matching( DOCUMENT_1 ) )
				.toQuery();
		assertThatQuery( query )
				.hasHitsAnyOrder( mainIndex.binding().string1Field.document1Value.indexedValue );

		// Conditional extensions with orElse - two, both unsupported
		query = scope.query()
				.select( f -> f.<String>extension()
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
				.where( f -> f.id().matching( DOCUMENT_1 ) )
				.toQuery();
		assertThatQuery( query )
				.hasHitsAnyOrder( mainIndex.binding().string1Field.document1Value.indexedValue );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void toAbsolutePath() {
		assertThat( mainIndex.createScope().projection().toAbsolutePath( "string" ) )
				.isEqualTo( "string" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void toAbsolutePath_withRoot() {
		assertThat( mainIndex.createScope().projection().withRoot( "nested" ).toAbsolutePath( "inner" ) )
				.isEqualTo( "nested.inner" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void toAbsolutePath_null() {
		assertThatThrownBy( () -> mainIndex.createScope().projection().toAbsolutePath( null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'relativeFieldPath' must not be null" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void toAbsolutePath_withRoot_null() {
		assertThatThrownBy( () -> mainIndex.createScope().projection().withRoot( "nested" ).toAbsolutePath( null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'relativeFieldPath' must not be null" );
	}

	private void initData() {
		mainIndex.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					mainIndex.binding().string1Field.document1Value.write( document );
					mainIndex.binding().string2Field.document1Value.write( document );

					mainIndex.binding().scoreField.document1Value.write( document );

					DocumentElement nestedDocument = document.addObject( mainIndex.binding().nestedObject );
					mainIndex.binding().nestedField.document1Value.write( nestedDocument );

					DocumentElement nestedNestedDocument = nestedDocument.addObject( mainIndex.binding().nestedNestedObject );
					mainIndex.binding().nestedNestedField.document1Value.write( nestedNestedDocument );

					DocumentElement flattedDocument = nestedDocument.addObject( mainIndex.binding().flattenedObject );
					mainIndex.binding().flattenedField.document1Value.write( flattedDocument );
				} )
				.add( DOCUMENT_2, document -> {
					mainIndex.binding().string1Field.document2Value.write( document );
					mainIndex.binding().string2Field.document2Value.write( document );

					mainIndex.binding().scoreField.document2Value.write( document );

					DocumentElement nestedDocument = document.addObject( mainIndex.binding().nestedObject );
					mainIndex.binding().nestedField.document2Value.write( nestedDocument );

					DocumentElement nestedNestedDocument = nestedDocument.addObject( mainIndex.binding().nestedNestedObject );
					mainIndex.binding().nestedNestedField.document2Value.write( nestedNestedDocument );

					DocumentElement flattedDocument = nestedDocument.addObject( mainIndex.binding().flattenedObject );
					mainIndex.binding().flattenedField.document2Value.write( flattedDocument );
				} )
				.add( DOCUMENT_3, document -> {
					mainIndex.binding().string1Field.document3Value.write( document );
					mainIndex.binding().string2Field.document3Value.write( document );

					mainIndex.binding().scoreField.document3Value.write( document );

					DocumentElement nestedDocument = document.addObject( mainIndex.binding().nestedObject );
					mainIndex.binding().nestedField.document3Value.write( nestedDocument );

					DocumentElement nestedNestedDocument = nestedDocument.addObject( mainIndex.binding().nestedNestedObject );
					mainIndex.binding().nestedNestedField.document3Value.write( nestedNestedDocument );

					DocumentElement flattedDocument = nestedDocument.addObject( mainIndex.binding().flattenedObject );
					mainIndex.binding().flattenedField.document3Value.write( flattedDocument );
				} )
				.add( EMPTY, document -> { } )
				.join();
	}

	private static <T, R> Function<T, R> shouldNotBeCalled() {
		return ignored -> {
			throw new IllegalStateException( "This should not be called" );
		};
	}

	private static class IndexBinding {
		final FieldModel<String> string1Field;
		final FieldModel<String> string2Field;
		final FieldModel<String> scoreField;

		final IndexObjectFieldReference nestedObject;
		final FieldModel<String> nestedField;

		final IndexObjectFieldReference nestedNestedObject;
		final FieldModel<String> nestedNestedField;

		final IndexObjectFieldReference flattenedObject;
		final FieldModel<String> flattenedField;

		IndexBinding(IndexSchemaElement root) {
			string1Field = FieldModel.mapper( String.class, "ccc", "mmm", "xxx" )
					.map( root, "string1" );
			string2Field = FieldModel.mapper( String.class, "ddd", "nnn", "yyy" )
					.map( root, "string2" );

			scoreField = FieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ),
					"scorepattern scorepattern", "scorepattern", "xxx" )
					.map( root, "score" );

			IndexSchemaObjectField nested = root.objectField( "nested", ObjectStructure.NESTED );
			nestedObject = nested.toReference();

			nestedField = FieldModel.mapper( String.class, "eee", "ooo", "zzz" )
					.map( nested, "inner" );

			IndexSchemaObjectField nestedNested = nested.objectField( "nested", ObjectStructure.NESTED );
			nestedNestedObject = nestedNested.toReference();

			nestedNestedField = FieldModel.mapper( String.class, "fff", "ppp", "aaa" )
					.map( nestedNested, "inner" );

			IndexSchemaObjectField flattened = nested.objectField( "flattened", ObjectStructure.FLATTENED );
			flattenedObject = flattened.toReference();

			flattenedField = FieldModel.mapper( String.class, "ggg", "ooo", "bbb" )
					.map( flattened, "inner" );
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
					c -> (StandardIndexFieldTypeOptionsStep<?, F>) c.as( type ),
					document1Value, document2Value, document3Value
			);
		}

		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> configuration,
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
			implements SearchProjectionFactoryExtension<MyExtendedFactory<R, E>, R, E> {
		@Override
		public Optional<MyExtendedFactory<R, E>> extendOptional(SearchProjectionFactory<R, E> original) {
			assertThat( original ).isNotNull();
			return Optional.of( new MyExtendedFactory<>( original ) );
		}
	}

	private static class UnSupportedExtension<R, E>
			implements SearchProjectionFactoryExtension<MyExtendedFactory<R, E>, R, E> {
		@Override
		public Optional<MyExtendedFactory<R, E>> extendOptional(SearchProjectionFactory<R, E> original) {
			assertThat( original ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedFactory<R, E> {
		private final SearchProjectionFactory<R, E> delegate;

		MyExtendedFactory(SearchProjectionFactory<R, E> delegate) {
			this.delegate = delegate;
		}

		public <T> ProjectionFinalStep<T> extendedProjection(String fieldName, Class<T> type) {
			return delegate.field( fieldName, type );
		}
	}
}
