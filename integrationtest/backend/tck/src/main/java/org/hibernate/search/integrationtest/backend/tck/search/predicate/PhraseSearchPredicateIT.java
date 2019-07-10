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
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.OverrideAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PhraseSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_NAME = "IndexWithCompatibleRawFields";
	private static final String INCOMPATIBLE_ANALYZER_INDEX_NAME = "IndexWithIncompatibleAnalyzer";
	private static final String UNSEARCHABLE_FIELDS_INDEX_NAME = "IndexWithUnsearchableFields";

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
	private static final String INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 = "incompatible_analyzer_1";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper( TckBackendHelper::createAnalysisOverrideBackendSetupStrategy );

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private OtherIndexMapping compatibleIndexMapping;
	private StubMappingIndexManager compatibleIndexManager;

	private OtherIndexMapping rawFieldCompatibleIndexMapping;
	private StubMappingIndexManager rawFieldCompatibleIndexManager;

	private OtherIndexMapping incompatibleAnalyzerIndexMapping;
	private StubMappingIndexManager incompatibleAnalyzerIndexManager;

	private StubMappingIndexManager unsearchableFieldsIndexManager;

	@Before
	public void setup() {
		setupHelper.start()
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
						INCOMPATIBLE_ANALYZER_INDEX_NAME,
						ctx -> this.incompatibleAnalyzerIndexMapping =
								OtherIndexMapping.createIncompatibleAnalyzer( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleAnalyzerIndexManager = indexManager
				)
				.withIndex(
						UNSEARCHABLE_FIELDS_INDEX_NAME,
						ctx -> OtherIndexMapping.createUnsearchableFieldsIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.unsearchableFieldsIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testPhraseQuery")
	public void phrase() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void phrase_unsearchable() {
		StubMappingScope scope = unsearchableFieldsIndexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException( () ->
				scope.predicate().phrase().onField( absoluteFieldPath )
		).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "is not searchable" )
				.hasMessageContaining( "Make sure the field is marked as searchable" )
				.hasMessageContaining( absoluteFieldPath );
	}

	/**
	 * Check that a phrase predicate can be used on a field that has a DSL converter.
	 * The DSL converter should be ignored, and there shouldn't be any exception thrown
	 * (the field should be considered as a text field).
	 */
	@Test
	public void withDslConverter() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringFieldWithDslConverter.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testPhraseQuery")
	public void singleTerm() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1_UNIQUE_TERM ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void emptyStringBeforeAnalysis() {
		StubMappingScope scope = indexManager.createScope();
		MainFieldModel fieldModel = indexMapping.analyzedStringField1;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.phrase().onField( fieldModel.relativeFieldName ).matching( "" ) )
				.toQuery();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	public void noTokenAfterAnalysis() {
		StubMappingScope scope = indexManager.createScope();
		MainFieldModel fieldModel = indexMapping.analyzedStringField1;

		SearchQuery<DocumentReference> query = scope.query()
				// Use stopwords, which should be removed by the analysis
				.predicate( f -> f.phrase().onField( fieldModel.relativeFieldName ).matching( "the a" ) )
				.toQuery();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	public void analyzerOverride() {
		StubMappingScope scope = indexManager.createScope();

		String whitespaceAnalyzedField = indexMapping.whitespaceAnalyzedField.relativeFieldName;
		String whitespaceLowercaseAnalyzedField = indexMapping.whitespaceLowercaseAnalyzedField.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.phrase().onField( whitespaceAnalyzedField ).matching( "ONCE UPON" ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		query = scope.query()
				.predicate( f -> f.phrase().onField( whitespaceLowercaseAnalyzedField ).matching( "ONCE UPON" ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = scope.query()
				.predicate( f -> f.phrase().onField( whitespaceAnalyzedField ).matching( "ONCE UPON" )
						.analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void analyzerOverride_notExistingName() {
		StubMappingScope scope = indexManager.createScope();
		String whitespaceAnalyzedField = indexMapping.whitespaceAnalyzedField.relativeFieldName;

		SubTest.expectException( () -> scope.query()
				.predicate( f -> f.phrase().onField( whitespaceAnalyzedField ).matching( "ONCE UPON" )
						// we don't have any analyzer with that name
						.analyzer( "this_name_does_actually_not_exist" ) )
				.toQuery().fetch()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "this_name_does_actually_not_exist" );
	}

	@Test
	public void skipAnalysis() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.whitespaceLowercaseAnalyzedField.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( "quick fox" ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// ignoring the analyzer means that the parameter of match predicate will not be tokenized
		// so it will not match any token
		query = scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( "quick fox" ).skipAnalysis() )
				.toQuery();

		assertThat( query )
				.hasNoHits();

		// to have a match with the skipAnalysis option enabled, we have to pass the parameter as a token is
		query = scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( "fox" ).skipAnalysis() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void error_unsupportedFieldType() {
		StubMappingScope scope = indexManager.createScope();

		for ( ByTypeFieldModel fieldModel : indexMapping.unsupportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					"phrase() predicate with unsupported type on field " + absoluteFieldPath,
					() -> scope.predicate().phrase().onField( absoluteFieldPath )
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
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				"phrase() predicate with null value to match",
				() -> scope.predicate().phrase().onField( absoluteFieldPath ).matching( null )
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
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;
		Function<Integer, SearchQuery<DocumentReference>> createQuery = slop -> scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1 ).withSlop( slop ) )
				.toQuery();

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
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				() -> scope.predicate().phrase()
						.onField( absoluteFieldPath ).boostedTo( 2.1f )
						.matching( PHRASE_1 )
						.withConstantScore()
						.toPredicate()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "per-field boosts together with withConstantScore option" );
	}

	@Test
	public void fieldLevelBoost() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.phrase()
						.onField( absoluteFieldPath1 ).boostedTo( 42 )
						.orField( absoluteFieldPath2 )
						.matching( PHRASE_1 )
				)
				.sort( f -> f.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );

		query = scope.query()
				.predicate( f -> f.phrase()
						.onField( absoluteFieldPath1 )
						.orField( absoluteFieldPath2 ).boostedTo( 42 )
						.matching( PHRASE_1 )
				)
				.sort( f -> f.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_5, DOCUMENT_1 );
	}

	@Test
	public void predicateLevelBoost() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						.should( f.phrase().onField( absoluteFieldPath1 )
								.matching( PHRASE_1 )
						)
						.should( f.phrase().onField( absoluteFieldPath2 )
								.matching( PHRASE_1 )
								.boostedTo( 7 )
						)
				)
				.sort( f -> f.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_5, DOCUMENT_1 );

		query = scope.query()
				.predicate( f -> f.bool()
						.should( f.phrase().onField( absoluteFieldPath1 )
								.matching( PHRASE_1 )
								.boostedTo( 39 )
						)
						.should( f.phrase().onField( absoluteFieldPath2 )
								.matching( PHRASE_1 )
						)
				)
				.sort( f -> f.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );
	}

	@Test
	public void predicateLevelBoost_withConstantScore() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						.should( f.phrase().onField( absoluteFieldPath1 )
								.matching( PHRASE_1 )
								.withConstantScore().boostedTo( 7 )
						)
						.should( f.phrase().onField( absoluteFieldPath2 )
								.matching( PHRASE_1 )
								.withConstantScore().boostedTo( 39 )
						)
				)
				.sort( f -> f.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_5, DOCUMENT_1 );

		query = scope.query()
				.predicate( f -> f.bool()
						.should( f.phrase().onField( absoluteFieldPath1 )
								.matching( PHRASE_1 )
								.withConstantScore().boostedTo( 39 )
						)
						.should( f.phrase().onField( absoluteFieldPath2 )
								.matching( PHRASE_1 )
								.withConstantScore().boostedTo( 7 )
						)
				)
				.sort( f -> f.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );
	}

	@Test
	public void multiFields() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;
		String absoluteFieldPath3 = indexMapping.analyzedStringField3.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery;

		// onField(...)

		createQuery = phrase -> scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath1 )
						.matching( phrase )
				)
				.toQuery();

		assertThat( createQuery.apply( PHRASE_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
		assertThat( createQuery.apply( PHRASE_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_3 ) )
				.hasNoHits();

		// onField(...).orField(...)

		createQuery = phrase -> scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath1 )
						.orField( absoluteFieldPath2 )
						.matching( phrase )
				)
				.toQuery();

		assertThat( createQuery.apply( PHRASE_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		// onField().orFields(...)

		createQuery = phrase -> scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath1 )
						.orFields( absoluteFieldPath2, absoluteFieldPath3 )
						.matching( phrase )
				)
				.toQuery();

		assertThat( createQuery.apply( PHRASE_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_4, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		// onFields(...)

		createQuery = phrase -> scope.query()
				.predicate( f -> f.phrase()
						.onFields( absoluteFieldPath1, absoluteFieldPath2 )
						.matching( phrase )
				)
				.toQuery();

		assertThat( createQuery.apply( PHRASE_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void error_unknownField() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				"phrase() predicate with unknown field",
				() -> scope.predicate().phrase().onField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"phrase() predicate with unknown field",
				() -> scope.predicate().phrase()
						.onFields( absoluteFieldPath, "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"phrase() predicate with unknown field",
				() -> scope.predicate().phrase().onField( absoluteFieldPath )
						.orField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"phrase() predicate with unknown field",
				() -> scope.predicate().phrase().onField( absoluteFieldPath )
						.orFields( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Test
	public void error_invalidSlop() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				"phrase() predicate with negative slop",
				() -> scope.predicate().phrase().onField( absoluteFieldPath )
						.matching( "foo" ).withSlop( -1 )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid slop" )
				.hasMessageContaining( "must be positive or zero" );

		SubTest.expectException(
				"phrase() predicate with negative slop",
				() -> scope.predicate().phrase().onField( absoluteFieldPath )
						.matching( "foo" ).withSlop( Integer.MIN_VALUE )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid slop" )
				.hasMessageContaining( "must be positive or zero" );
	}

	@Test
	public void multiIndex_withCompatibleIndexManager() {
		StubMappingScope scope = indexManager.createScope(
				compatibleIndexManager
		);

		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1 );
			b.doc( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager() {
		StubMappingScope scope = indexManager.createScope( rawFieldCompatibleIndexManager );
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1 );
			b.doc( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer() {
		StubMappingScope scope = indexManager.createScope( incompatibleAnalyzerIndexManager );
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				() -> {
					scope.query()
							.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1_UNIQUE_TERM ) )
							.toQuery();
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( absoluteFieldPath )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_ANALYZER_INDEX_NAME )
				) )
		;
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_overrideAnalyzer() {
		StubMappingScope scope = indexManager.createScope( incompatibleAnalyzerIndexManager );
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1_UNIQUE_TERM )
						.analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
			b.doc( INCOMPATIBLE_ANALYZER_INDEX_NAME, INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_skipAnalysis() {
		StubMappingScope scope = indexManager.createScope( incompatibleAnalyzerIndexManager );
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.phrase().onField( absoluteFieldPath ).matching( PHRASE_1_UNIQUE_TERM )
						.skipAnalysis() )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
			b.doc( INCOMPATIBLE_ANALYZER_INDEX_NAME, INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_incompatibleSearchable() {
		StubMappingScope scope = indexManager.createScope( unsearchableFieldsIndexManager );
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException( () -> scope.predicate().phrase().onField( absoluteFieldPath ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( absoluteFieldPath )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, UNSEARCHABLE_FIELDS_INDEX_NAME )
				) )
		;
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, PHRASE_1_TEXT_EXACT_MATCH );
			document.addValue( indexMapping.analyzedStringFieldWithDslConverter.reference, PHRASE_1_TEXT_EXACT_MATCH );
			document.addValue( indexMapping.whitespaceAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH.toLowerCase( Locale.ROOT ) );
			document.addValue( indexMapping.whitespaceLowercaseAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH.toLowerCase( Locale.ROOT ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, PHRASE_1_TEXT_SLOP_1_MATCH );
			document.addValue( indexMapping.analyzedStringField2.reference, PHRASE_2_TEXT_EXACT_MATCH );
			document.addValue( indexMapping.whitespaceAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH.toUpperCase( Locale.ROOT ) );
			document.addValue( indexMapping.whitespaceLowercaseAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH.toUpperCase( Locale.ROOT ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, PHRASE_1_TEXT_SLOP_2_MATCH );
			document.addValue( indexMapping.analyzedStringField2.reference, PHRASE_3_TEXT_EXACT_MATCH );
			document.addValue( indexMapping.analyzedStringField3.reference, PHRASE_1_TEXT_EXACT_MATCH );
			document.addValue( indexMapping.whitespaceAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH );
			document.addValue( indexMapping.whitespaceLowercaseAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH );
		} );
		workPlan.add( referenceProvider( DOCUMENT_4 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, PHRASE_1_TEXT_SLOP_3_MATCH );
			document.addValue( indexMapping.analyzedStringField3.reference, PHRASE_2_TEXT_EXACT_MATCH );
		} );
		workPlan.add( referenceProvider( DOCUMENT_5 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, PHRASE_2_TEXT_EXACT_MATCH );
			document.addValue( indexMapping.analyzedStringField2.reference, PHRASE_1_TEXT_EXACT_MATCH );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> {
		} );
		workPlan.execute().join();

		workPlan = compatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			document.addValue( compatibleIndexMapping.analyzedStringField1.reference, PHRASE_1_TEXT_EXACT_MATCH );
		} );
		workPlan.execute().join();

		workPlan = rawFieldCompatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			document.addValue( rawFieldCompatibleIndexMapping.analyzedStringField1.reference, PHRASE_1_TEXT_EXACT_MATCH );
		} );
		workPlan.execute().join();

		workPlan = incompatibleAnalyzerIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 ), document -> {
			document.addValue( incompatibleAnalyzerIndexMapping.analyzedStringField1.reference, PHRASE_1_TEXT_EXACT_MATCH );
		} );
		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4, DOCUMENT_5, EMPTY );
		query = compatibleIndexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
		query = rawFieldCompatibleIndexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
		query = incompatibleAnalyzerIndexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INCOMPATIBLE_ANALYZER_INDEX_NAME, INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 );
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
		final MainFieldModel whitespaceAnalyzedField;
		final MainFieldModel whitespaceLowercaseAnalyzedField;

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
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString1" );
			analyzedStringField2 = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString2" );
			analyzedStringField3 = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString3" );
			analyzedStringFieldWithDslConverter = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
							.dslConverter( ValueWrapper.toIndexFieldConverter() )
			)
					.map( root, "analyzedStringWithDslConverter" );
			whitespaceAnalyzedField = MainFieldModel.mapper(
					c -> c.asString().analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE.name )
			)
					.map( root, "whitespaceAnalyzed" );
			whitespaceLowercaseAnalyzedField = MainFieldModel.mapper(
					c -> c.asString().analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
			)
					.map( root, "whitespaceLowercaseAnalyzed" );
		}
	}

	private static class OtherIndexMapping {
		static OtherIndexMapping createCompatible(IndexSchemaElement root) {
			return new OtherIndexMapping(
					MainFieldModel.mapper(
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexMapping createRawFieldCompatible(IndexSchemaElement root) {
			return new OtherIndexMapping(
					MainFieldModel.mapper(
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
									// Using a different DSL converter
									.dslConverter( ValueWrapper.toIndexFieldConverter() )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexMapping createIncompatibleAnalyzer(IndexSchemaElement root) {
			return new OtherIndexMapping(
					MainFieldModel.mapper(
							// Using a different analyzer
							c -> c.asString().analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexMapping createUnsearchableFieldsIndexMapping(IndexSchemaElement root) {
			return new OtherIndexMapping(
					MainFieldModel.mapper(
							// make the field not searchable
							c -> c.asString().searchable( Searchable.NO )
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
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, String>> configuration) {
			return StandardFieldMapper.of(
					configuration,
					(reference, name) -> new MainFieldModel( reference, name )
			);
		}

		final IndexFieldReference<String> reference;
		final String relativeFieldName;

		private MainFieldModel(IndexFieldReference<String> reference, String relativeFieldName) {
			this.reference = reference;
			this.relativeFieldName = relativeFieldName;
		}
	}

	private static class ByTypeFieldModel {
		static <F> StandardFieldMapper<F, ByTypeFieldModel> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					(reference, name) -> new ByTypeFieldModel( name )
			);
		}

		final String relativeFieldName;

		private ByTypeFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}
}