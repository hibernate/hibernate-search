/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.EasyMockUtils.referenceMatcher;
import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.integrationtest.backend.tck.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.search.StubDocumentReferenceTransformer;
import org.hibernate.search.integrationtest.backend.tck.search.StubLoadedObject;
import org.hibernate.search.integrationtest.backend.tck.search.StubObjectLoader;
import org.hibernate.search.integrationtest.backend.tck.search.StubTransformedReference;
import org.hibernate.search.integrationtest.backend.tck.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.EasyMockUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.GenericStubMappingSearchTarget;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.assertj.core.api.Assertions;
import org.easymock.EasyMock;

/**
 * Generic tests for projections. More specific tests can be found in other classes, such as {@link FieldSearchProjectionIT}.
 */
public class SearchProjectionIT {

	private static final String INDEX_NAME = "IndexName";

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

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void noProjections() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<List<?>> query = searchTarget.query()
				.asProjections()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();

		assertThat( query ).hasHitCount( 4 );
	}

	@Test
	public void references() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

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
				searchTarget.projection().documentReference().toProjection();
		SearchProjection<DocumentReference> referenceProjection =
				searchTarget.projection().reference().toProjection();
		SearchProjection<DocumentReference> objectProjection =
				searchTarget.projection().object().toProjection();

		query = searchTarget.query()
				.asProjections(
						documentReferenceProjection,
						referenceProjection,
						objectProjection
				)
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> {
			b.list( document1Reference, document1Reference, document1Reference );
			b.list( document2Reference, document2Reference, document2Reference );
			b.list( document3Reference, document3Reference, document3Reference );
			b.list( emptyReference, emptyReference, emptyReference );
		} );
	}

	/**
	 * Test documentReference/reference/object projections as they are likely to be used by mappers,
	 * i.e. with a custom reference transformer and a custom object loader.
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

		Function<DocumentReference, StubTransformedReference> referenceTransformerMock =
				EasyMock.createMock( StubDocumentReferenceTransformer.class );
		ObjectLoader<StubTransformedReference, StubLoadedObject> objectLoaderMock =
				EasyMock.createMock( StubObjectLoader.class );

		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( document1Reference ) ) )
				.andReturn( document1TransformedReference )
				.times( 2 );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( document2Reference ) ) )
				.andReturn( document2TransformedReference )
				.times( 2 );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( document3Reference ) ) )
				.andReturn( document3TransformedReference )
				.times( 2 );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( emptyReference ) ) )
				.andReturn( emptyTransformedReference )
				.times( 2 );
		EasyMock.expect( objectLoaderMock.load(
				EasyMockUtils.collectionAnyOrderMatcher( Arrays.asList(
						document1TransformedReference, document2TransformedReference,
						document3TransformedReference, emptyTransformedReference
				) )
		) )
				.andReturn( Arrays.asList(
						document1LoadedObject, document2LoadedObject,
						document3LoadedObject, emptyLoadedObject
				) );
		EasyMock.replay( referenceTransformerMock, objectLoaderMock );

		GenericStubMappingSearchTarget<StubTransformedReference, StubLoadedObject> searchTarget =
				indexManager.createSearchTarget( referenceTransformerMock );
		SearchQuery<List<?>> query;

		/*
		 * Note to test writers: make sure to assign these projections to variables,
		 * just so that tests do not compile if someone changes the APIs in an incorrect way.
		 */
		SearchProjection<DocumentReference> documentReferenceProjection =
				searchTarget.projection().documentReference().toProjection();
		SearchProjection<StubTransformedReference> referenceProjection =
				searchTarget.projection().reference().toProjection();
		SearchProjection<StubLoadedObject> objectProjection =
				searchTarget.projection().object().toProjection();

		query = searchTarget.query( objectLoaderMock )
				.asProjections(
						documentReferenceProjection,
						referenceProjection,
						objectProjection
				)
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> {
			b.list( document1Reference, document1TransformedReference, document1LoadedObject );
			b.list( document2Reference, document2TransformedReference, document2LoadedObject );
			b.list( document3Reference, document3TransformedReference, document3LoadedObject );
			b.list( emptyReference, emptyTransformedReference, emptyLoadedObject );
		} );

		EasyMock.verify( referenceTransformerMock, objectLoaderMock );
	}

	@Test
	public void score() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<Float> query = searchTarget.query()
				.asProjection( f -> f.score().toProjection() )
				.predicate( f -> f.match().onField( indexMapping.scoreField.relativeFieldName ).matching( "scorepattern" ).toPredicate() )
				.sort( c -> c.byScore().desc() )
				.build();

		SearchResult<Float> result = query.execute();
		assertThat( result ).hasHitCount( 2 );

		Float score1 = result.getHits().get( 0 );
		Float score2 = result.getHits().get( 1 );

		Assertions.assertThat( score1 ).isNotNull();
		Assertions.assertThat( score2 ).isNotNull();

		Assertions.assertThat( score1 ).isGreaterThan( score2 );
	}

	/**
	 * Test mixing multiple projection types (field projections, special projections, ...),
	 * and also multiple field projections.
	 */
	@Test
	public void mixed() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<List<?>> query;

		query = searchTarget.query()
				.asProjection( f ->
						f.composite(
								f.field( indexMapping.string1Field.relativeFieldName, String.class ),
								f.documentReference(),
								f.field( indexMapping.string2Field.relativeFieldName, String.class )
						)
						.toProjection()
				)
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
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
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
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

			scoreField = FieldModel.mapper( String.class,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name ),
					"scorepattern scorepattern", "scorepattern", "xxx" )
					.map( root, "score" );
		}
	}

	private static class ValueModel<F> {
		private final IndexFieldAccessor<F> accessor;
		final F indexedValue;

		private ValueModel(IndexFieldAccessor<F> accessor, F indexedValue) {
			this.accessor = accessor;
			this.indexedValue = indexedValue;
		}

		public void write(DocumentElement target) {
			accessor.write( target, indexedValue );
		}
	}

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(Class<F> type,
				F document1Value, F document2Value, F document3Value) {
			return mapper(
					type,
					c -> (StandardIndexSchemaFieldTypedContext<?, F>) c.as( type ),
					document1Value, document2Value, document3Value
			);
		}

		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(Class<F> type,
				Function<IndexSchemaFieldContext, StandardIndexSchemaFieldTypedContext<?, F>> configuration,
				F document1Value, F document2Value, F document3Value) {
			return (parent, name, additionalConfiguration) -> {
				IndexSchemaFieldContext untypedContext = parent.field( name );
				StandardIndexSchemaFieldTypedContext<?, F> context = configuration.apply( untypedContext );
				context.projectable( Projectable.YES );
				additionalConfiguration.accept( context );
				IndexFieldAccessor<F> accessor = context.createAccessor();
				return new FieldModel<>(
						accessor, name, type,
						document1Value, document2Value, document3Value
				);
			};
		}

		final String relativeFieldName;
		final Class<F> type;

		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;
		final ValueModel<F> document3Value;

		private FieldModel(IndexFieldAccessor<F> accessor, String relativeFieldName, Class<F> type,
				F document1Value, F document2Value, F document3Value) {
			this.relativeFieldName = relativeFieldName;
			this.type = type;
			this.document1Value = new ValueModel<>( accessor, document1Value );
			this.document2Value = new ValueModel<>( accessor, document2Value );
			this.document3Value = new ValueModel<>( accessor, document3Value );
		}
	}
}
