/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PhraseSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_NAME = "IndexWithCompatibleRawFields";
	private static final String INCOMPATIBLE_INDEX_NAME = "IndexWithIncompatibleFields";

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String DOCUMENT_4 = "document4";
	private static final String DOCUMENT_5 = "document5";
	private static final String EMPTY = "empty";

	private static final String PHRASE_1 = "quick fox";
	private static final String PHRASE_1_UNIQUE_TERM = "fox";
	private static final String PHRASE_1_TEXT_EXACT_MATCH = "Once upon a time, there was a quick fox in a big house.";
	private static final String PHRASE_1_TEXT_SLOP_1_MATCH = "Once upon a time, there was a quick brown fox in a big house.";
	private static final String PHRASE_1_TEXT_SLOP_2_MATCH = "Once upon a time, there was a quick, sad brown fox in a big house.";
	private static final String PHRASE_1_TEXT_SLOP_3_MATCH = "In the big house, the fox was quick.";

	private static final String PHRASE_2 = "white shark";
	private static final String PHRASE_2_TEXT_EXACT_MATCH = "Once upon a time, there was a white shark in a small sea.";

	private static final String PHRASE_3 = "sly cat";
	private static final String PHRASE_3_TEXT_EXACT_MATCH = "Once upon a time, there was a sly cat in a dark tree.";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";


	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private OtherIndexMapping compatibleIndexMapping;
	private StubMappingIndexManager compatibleIndexManager;

	private OtherIndexMapping rawFieldCompatibleIndexMapping;
	private StubMappingIndexManager rawFieldCompatibleIndexManager;

	private StubMappingIndexManager incompatibleIndexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						COMPATIBLE_INDEX_NAME,
						ctx -> this.compatibleIndexMapping =
								OtherIndexMapping.createCompatible( ctx.getSchemaElement() ),
						indexManager -> this.compatibleIndexManager = indexManager
				)
				.withIndex(
						RAW_FIELD_COMPATIBLE_INDEX_NAME,
						ctx -> this.rawFieldCompatibleIndexMapping =
								OtherIndexMapping.createRawFieldCompatible( ctx.getSchemaElement() ),
						indexManager -> this.rawFieldCompatibleIndexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_INDEX_NAME,
						ctx -> OtherIndexMapping.createIncompatible( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testPhraseQuery")
	public void phrase() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1 ) )
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	/**
	 * Check that a phrase predicate can be used on a field that has a DSL converter.
	 * The DSL converter should be ignored, and there shouldn't be any exception thrown
	 * (the field should be considered as a text field).
	 */
	@Test
	public void withDslConverter() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		String absoluteFieldPath = indexMapping.analyzedStringFieldWithDslConverter.relativeFieldName;

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1 ) )
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testPhraseQuery")
	public void singleTerm() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1_UNIQUE_TERM ) )
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void emptyStringBeforeAnalysis() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		MainFieldModel fieldModel = indexMapping.analyzedStringField1;

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.phrase().onField( fieldModel.relativeFieldName ).matching( "" ) )
				.build();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	public void noTokenAfterAnalysis() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		MainFieldModel fieldModel = indexMapping.analyzedStringField1;

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				// Use stopwords, which should be removed by the analysis
				.predicate( f -> f.phrase().onField( fieldModel.relativeFieldName ).matching( "the a" ) )
				.build();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	public void error_unsupportedFieldType() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel fieldModel : indexMapping.unsupportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					"phrase() predicate with unsupported type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().phrase().onField( absoluteFieldPath )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Text predicates" )
					.hasMessageContaining( "are not supported by" )
					.hasMessageContaining( "'" + absoluteFieldPath + "'" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );
		}
	}

	@Test
	public void error_null() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				"phrase() predicate with null value to match",
				() -> searchTarget.predicate().phrase().onField( absoluteFieldPath ).matching( null )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid phrase" )
				.hasMessageContaining( "must be non-null" )
				.hasMessageContaining( absoluteFieldPath );
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testPhraseQuery")
	public void slop() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;
		Function<Integer, SearchQuery<DocumentReference>> createQuery = slop -> searchTarget.query().asReference()
				.predicate( f -> f.phrase().withSlop( slop ).onField( absoluteFieldPath ).matching( PHRASE_1 ) )
				.build();

		assertThat( createQuery.apply( 0 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		assertThat( createQuery.apply( 1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		assertThat( createQuery.apply( 2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		assertThat( createQuery.apply( 3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );

		assertThat( createQuery.apply( 50 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void perFieldBoostWithConstantScore_error() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				() -> searchTarget.predicate().phrase().withConstantScore()
						.onField( absoluteFieldPath ).boostedTo( 2.1f )
						.matching( PHRASE_1 )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "per-field boosts together with withConstantScore option" );
	}

	@Test
	public void fieldLevelBoost() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.phrase()
						.onField( absoluteFieldPath1 ).boostedTo( 42 )
						.orField( absoluteFieldPath2 )
						.matching( PHRASE_1 )
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.phrase()
						.onField( absoluteFieldPath1 )
						.orField( absoluteFieldPath2 ).boostedTo( 42 )
						.matching( PHRASE_1 )
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_5, DOCUMENT_1 );
	}

	@Test
	public void predicateLevelBoost() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.phrase().onField( absoluteFieldPath1 )
								.matching( PHRASE_1 )
						)
						.should( f.phrase().boostedTo( 7 ).onField( absoluteFieldPath2 )
								.matching( PHRASE_1 )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_5, DOCUMENT_1 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.phrase().boostedTo( 39 ).onField( absoluteFieldPath1 )
								.matching( PHRASE_1 )
						)
						.should( f.phrase().onField( absoluteFieldPath2 )
								.matching( PHRASE_1 )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );
	}

	@Test
	public void predicateLevelBoost_withConstantScore() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.phrase().withConstantScore().boostedTo( 7 )
								.onField( absoluteFieldPath1 )
								.matching( PHRASE_1 )
						)
						.should( f.phrase().withConstantScore().boostedTo( 39 )
								.onField( absoluteFieldPath2 )
								.matching( PHRASE_1 )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_5, DOCUMENT_1 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.phrase().withConstantScore().boostedTo( 39 )
								.onField( absoluteFieldPath1 )
								.matching( PHRASE_1 )
						)
						.should( f.phrase().withConstantScore().boostedTo( 7 )
								.onField( absoluteFieldPath2 )
								.matching( PHRASE_1 )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );
	}

	@Test
	public void multiFields() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;
		String absoluteFieldPath3 = indexMapping.analyzedStringField3.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery;

		// onField(...)

		createQuery = phrase -> searchTarget.query().asReference()
				.predicate( f -> f.phrase().onField( absoluteFieldPath1 )
						.matching( phrase )
				)
				.build();

		assertThat( createQuery.apply( PHRASE_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
		assertThat( createQuery.apply( PHRASE_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_3 ) )
				.hasNoHits();

		// onField(...).orField(...)

		createQuery = phrase -> searchTarget.query().asReference()
				.predicate( f -> f.phrase().onField( absoluteFieldPath1 )
						.orField( absoluteFieldPath2 )
						.matching( phrase )
				)
				.build();

		assertThat( createQuery.apply( PHRASE_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		// onField().orFields(...)

		createQuery = phrase -> searchTarget.query()
				.asReference()
				.predicate( f -> f.phrase().onField( absoluteFieldPath1 )
						.orFields( absoluteFieldPath2, absoluteFieldPath3 )
						.matching( phrase )
				)
				.build();

		assertThat( createQuery.apply( PHRASE_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_4, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		// onFields(...)

		createQuery = phrase -> searchTarget.query()
				.asReference()
				.predicate( f -> f.phrase()
						.onFields( absoluteFieldPath1, absoluteFieldPath2 )
						.matching( phrase )
				)
				.build();

		assertThat( createQuery.apply( PHRASE_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void error_unknownField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				"phrase() predicate with unknown field",
				() -> searchTarget.predicate().phrase().onField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"phrase() predicate with unknown field",
				() -> searchTarget.predicate().phrase()
						.onFields( absoluteFieldPath, "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"phrase() predicate with unknown field",
				() -> searchTarget.predicate().phrase().onField( absoluteFieldPath )
						.orField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"phrase() predicate with unknown field",
				() -> searchTarget.predicate().phrase().onField( absoluteFieldPath )
						.orFields( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Test
	public void error_invalidSlop() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SubTest.expectException(
				"phrase() predicate with negative slop",
				() -> searchTarget.predicate().phrase().withSlop( -1 )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid slop" )
				.hasMessageContaining( "must be positive or zero" );

		SubTest.expectException(
				"phrase() predicate with negative slop",
				() -> searchTarget.predicate().phrase().withSlop( Integer.MIN_VALUE )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid slop" )
				.hasMessageContaining( "must be positive or zero" );
	}

	@Test
	public void multiIndex_withCompatibleIndexManager() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget(
				compatibleIndexManager
		);

		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1 ) )
				.build();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1 );
			b.doc( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( rawFieldCompatibleIndexManager );

		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1 ) )
				.build();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1 );
			b.doc( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withIncompatibleIndexManager() {
		// TODO HSEARCH-3307 re-enable this test once we properly take analyzer/normalizer into account when testing field compatibility for predicates in Elasticsearch
		Assume.assumeTrue( "This feature is not implemented yet", false );

		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				() -> {
					indexManager.createSearchTarget( incompatibleIndexManager )
							.predicate().phrase().onField( absoluteFieldPath );
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( "'" + absoluteFieldPath + "'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_INDEX_NAME )
				) );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.analyzedStringField1.accessor.write( document, PHRASE_1_TEXT_EXACT_MATCH );
			indexMapping.analyzedStringFieldWithDslConverter.accessor.write( document, PHRASE_1_TEXT_EXACT_MATCH );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.analyzedStringField1.accessor.write( document, PHRASE_1_TEXT_SLOP_1_MATCH );
			indexMapping.analyzedStringField2.accessor.write( document, PHRASE_2_TEXT_EXACT_MATCH );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.analyzedStringField1.accessor.write( document, PHRASE_1_TEXT_SLOP_2_MATCH );
			indexMapping.analyzedStringField2.accessor.write( document, PHRASE_3_TEXT_EXACT_MATCH );
			indexMapping.analyzedStringField3.accessor.write( document, PHRASE_1_TEXT_EXACT_MATCH );
		} );
		workPlan.add( referenceProvider( DOCUMENT_4 ), document -> {
			indexMapping.analyzedStringField1.accessor.write( document, PHRASE_1_TEXT_SLOP_3_MATCH );
			indexMapping.analyzedStringField3.accessor.write( document, PHRASE_2_TEXT_EXACT_MATCH );
		} );
		workPlan.add( referenceProvider( DOCUMENT_5 ), document -> {
			indexMapping.analyzedStringField1.accessor.write( document, PHRASE_2_TEXT_EXACT_MATCH );
			indexMapping.analyzedStringField2.accessor.write( document, PHRASE_1_TEXT_EXACT_MATCH );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> {
		} );
		workPlan.execute().join();

		workPlan = compatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			compatibleIndexMapping.analyzedStringField1.accessor.write( document, PHRASE_1_TEXT_EXACT_MATCH );
		} );
		workPlan.execute().join();

		workPlan = rawFieldCompatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			rawFieldCompatibleIndexMapping.analyzedStringField1.accessor.write( document, PHRASE_1_TEXT_EXACT_MATCH );
		} );
		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4, DOCUMENT_5, EMPTY );
		query = compatibleIndexManager.createSearchTarget().query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
		query = rawFieldCompatibleIndexManager.createSearchTarget().query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
	}

	private static void forEachTypeDescriptor(Consumer<FieldTypeDescriptor<?>> action) {
		FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getMatchPredicateExpectations().isPresent() )
				.forEach( action );
	}

	private static void mapByTypeFields(IndexSchemaElement parent, String prefix,
			FieldModelConsumer<Void, ByTypeFieldModel> consumer) {
		forEachTypeDescriptor( typeDescriptor -> {
			ByTypeFieldModel fieldModel = ByTypeFieldModel.mapper( typeDescriptor )
					.map( parent, prefix + typeDescriptor.getUniqueName() );
			consumer.accept( typeDescriptor, null, fieldModel );
		} );
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel> unsupportedFieldModels = new ArrayList<>();

		final MainFieldModel analyzedStringField1;
		final MainFieldModel analyzedStringField2;
		final MainFieldModel analyzedStringField3;
		final MainFieldModel analyzedStringFieldWithDslConverter;

		IndexMapping(IndexSchemaElement root) {
			mapByTypeFields(
					root, "byType_",
					(typeDescriptor, ignored, model) -> {
						if ( !String.class.equals( typeDescriptor.getJavaType() ) ) {
							unsupportedFieldModels.add( model );
						}
					}
			);
			analyzedStringField1 = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name )
			)
					.map( root, "analyzedString1" );
			analyzedStringField2 = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name )
			)
					.map( root, "analyzedString2" );
			analyzedStringField3 = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name )
			)
					.map( root, "analyzedString3" );
			analyzedStringFieldWithDslConverter = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name )
							.dslConverter( ValueWrapper.toIndexFieldConverter() )
			)
					.map( root, "analyzedStringWithDslConverter" );
		}
	}

	private static class OtherIndexMapping {
		static OtherIndexMapping createCompatible(IndexSchemaElement root) {
			return new OtherIndexMapping(
					MainFieldModel.mapper(
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexMapping createRawFieldCompatible(IndexSchemaElement root) {
			return new OtherIndexMapping(
					MainFieldModel.mapper(
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name )
									// Using a different DSL converter
									.dslConverter( ValueWrapper.toIndexFieldConverter() )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexMapping createIncompatible(IndexSchemaElement root) {
			return new OtherIndexMapping(
					MainFieldModel.mapper(
							// Using a different analyzer/normalizer
							c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name )
					)
							.map( root, "analyzedString1" )
			);
		}

		final MainFieldModel analyzedStringField1;

		private OtherIndexMapping(MainFieldModel analyzedStringField1) {
			this.analyzedStringField1 = analyzedStringField1;
		}
	}

	private static class MainFieldModel {
		static StandardFieldMapper<String, MainFieldModel> mapper(
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, String>> configuration) {
			return StandardFieldMapper.of(
					configuration,
					(accessor, name) -> new MainFieldModel( accessor, name )
			);
		}

		final IndexFieldAccessor<String> accessor;
		final String relativeFieldName;

		private MainFieldModel(IndexFieldAccessor<String> accessor, String relativeFieldName) {
			this.accessor = accessor;
			this.relativeFieldName = relativeFieldName;
		}
	}

	private static class ByTypeFieldModel {
		static <F> StandardFieldMapper<F, ByTypeFieldModel> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					(accessor, name) -> new ByTypeFieldModel( name )
			);
		}

		final String relativeFieldName;

		private ByTypeFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}
}