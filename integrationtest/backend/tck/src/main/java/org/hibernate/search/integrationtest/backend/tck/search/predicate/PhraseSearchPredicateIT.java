/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PhraseSearchPredicateIT {

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
	private static final String COMPATIBLE_SEARCH_ANALYZER_INDEX_DOCUMENT_1 = "compatible_search_analyzer_1";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final SimpleMappedIndex<OtherIndexBinding> compatibleIndex =
			SimpleMappedIndex.of( OtherIndexBinding::createCompatible ).name( "compatible" );
	private final SimpleMappedIndex<OtherIndexBinding> rawFieldCompatibleIndex =
			SimpleMappedIndex.of( OtherIndexBinding::createRawFieldCompatible ).name( "rawFieldCompatible" );
	private final SimpleMappedIndex<OtherIndexBinding> compatibleSearchAnalyzerIndex =
			SimpleMappedIndex.of( OtherIndexBinding::createCompatibleSearchAnalyzer ).name( "compatibleSearchAnalyzer" );
	private final SimpleMappedIndex<OtherIndexBinding> incompatibleAnalyzerIndex =
			SimpleMappedIndex.of( OtherIndexBinding::createIncompatibleAnalyzer ).name( "incompatibleAnalyzer" );
	private final SimpleMappedIndex<OtherIndexBinding> unsearchableFieldsIndex =
			SimpleMappedIndex.of( OtherIndexBinding::createUnsearchableFieldsIndexBinding ).name( "unsearchableFields" );

	@Before
	public void setup() {
		setupHelper.start()
				.withIndexes(
						mainIndex, compatibleIndex, rawFieldCompatibleIndex, compatibleSearchAnalyzerIndex,
						incompatibleAnalyzerIndex, unsearchableFieldsIndex
				)
				.setup();

		initData();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testPhraseQuery")
	public void phrase() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	public void phrase_nonAnalyzedField() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().nonAnalyzedField.relativeFieldName;

		assertThat( scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1 ) )
				.toQuery() )
				.hasNoHits();

		assertThat( scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1_TEXT_EXACT_MATCH ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	public void phrase_unsearchable() {
		StubMappingScope scope = unsearchableFieldsIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		Assertions.assertThatThrownBy( () ->
				scope.predicate().phrase().field( absoluteFieldPath )
		)
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
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringFieldWithDslConverter.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testPhraseQuery")
	public void singleTerm() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1_UNIQUE_TERM ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void emptyStringBeforeAnalysis() {
		StubMappingScope scope = mainIndex.createScope();
		MainFieldModel fieldModel = mainIndex.binding().analyzedStringField1;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.phrase().field( fieldModel.relativeFieldName ).matching( "" ) )
				.toQuery();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	public void noTokenAfterAnalysis() {
		StubMappingScope scope = mainIndex.createScope();
		MainFieldModel fieldModel = mainIndex.binding().analyzedStringField1;

		SearchQuery<DocumentReference> query = scope.query()
				// Use stopwords, which should be removed by the analysis
				.where( f -> f.phrase().field( fieldModel.relativeFieldName ).matching( "the a" ) )
				.toQuery();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	public void analyzerOverride() {
		StubMappingScope scope = mainIndex.createScope();

		String whitespaceAnalyzedField = mainIndex.binding().whitespaceAnalyzedField.relativeFieldName;
		String whitespaceLowercaseAnalyzedField = mainIndex.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;
		String whitespaceLowercaseSearchAnalyzedField = mainIndex.binding().whitespaceLowercaseSearchAnalyzedField.relativeFieldName;

		// Terms are never lower-cased, neither at write nor at query time.
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.phrase().field( whitespaceAnalyzedField ).matching( "ONCE UPON" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );

		// Terms are always lower-cased, both at write and at query time.
		query = scope.query()
				.where( f -> f.phrase().field( whitespaceLowercaseAnalyzedField ).matching( "ONCE UPON" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// Terms are lower-cased only at query time. Because we are overriding the analyzer in the predicate.
		query = scope.query()
				.where( f -> f.phrase().field( whitespaceAnalyzedField ).matching( "ONCE UPON" )
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// Same here. Terms are lower-cased only at query time. Because we've defined a search analyzer.
		query = scope.query()
				.where( f -> f.phrase().field( whitespaceLowercaseSearchAnalyzedField ).matching( "ONCE UPON" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// As for the first query, terms are never lower-cased, neither at write nor at query time.
		// Because even if we've defined a search analyzer, we are overriding it with an analyzer in the predicate,
		// since the overriding takes precedence over the search analyzer.
		query = scope.query()
				.where( f -> f.phrase().field( whitespaceLowercaseSearchAnalyzedField ).matching( "ONCE UPON" )
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
	}

	@Test
	public void analyzerOverride_notExistingName() {
		StubMappingScope scope = mainIndex.createScope();
		String whitespaceAnalyzedField = mainIndex.binding().whitespaceAnalyzedField.relativeFieldName;

		Assertions.assertThatThrownBy( () -> scope.query()
				.where( f -> f.phrase().field( whitespaceAnalyzedField ).matching( "ONCE UPON" )
						// we don't have any analyzer with that name
						.analyzer( "this_name_does_actually_not_exist" ) )
				.toQuery().fetchAll()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "this_name_does_actually_not_exist" );
	}

	@Test
	public void skipAnalysis() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( "quick fox" ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// ignoring the analyzer means that the parameter of match predicate will not be tokenized
		// so it will not match any token
		query = scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( "quick fox" ).skipAnalysis() )
				.toQuery();

		assertThat( query )
				.hasNoHits();

		// to have a match with the skipAnalysis option enabled, we have to pass the parameter as a token is
		query = scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( "fox" ).skipAnalysis() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void error_unsupportedFieldType() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel fieldModel : mainIndex.binding().unsupportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			Assertions.assertThatThrownBy(
					() -> scope.predicate().phrase().field( absoluteFieldPath ),
					"phrase() predicate with unsupported type on field " + absoluteFieldPath
			)
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
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		Assertions.assertThatThrownBy(
				() -> scope.predicate().phrase().field( absoluteFieldPath ).matching( null ),
				"phrase() predicate with null value to match"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid phrase" )
				.hasMessageContaining( "must be non-null" )
				.hasMessageContaining( absoluteFieldPath );
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testPhraseQuery")
	public void slop() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;
		Function<Integer, SearchQuery<DocumentReference>> createQuery = slop -> scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1 ).slop( slop ) )
				.toQuery();

		assertThat( createQuery.apply( 0 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		assertThat( createQuery.apply( 1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );

		assertThat( createQuery.apply( 2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		assertThat( createQuery.apply( 3 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );

		assertThat( createQuery.apply( 50 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void perFieldBoostWithConstantScore_error() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		Assertions.assertThatThrownBy(
				() -> scope.predicate().phrase()
						.field( absoluteFieldPath ).boost( 2.1f )
						.matching( PHRASE_1 )
						.constantScore()
						.toPredicate()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "per-field boosts together with withConstantScore option" );
	}

	@Test
	public void fieldLevelBoost() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath1 = mainIndex.binding().analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = mainIndex.binding().analyzedStringField2.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.phrase()
						.field( absoluteFieldPath1 ).boost( 42 )
						.field( absoluteFieldPath2 )
						.matching( PHRASE_1 )
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_5 );

		query = scope.query()
				.where( f -> f.phrase()
						.field( absoluteFieldPath1 )
						.field( absoluteFieldPath2 ).boost( 42 )
						.matching( PHRASE_1 )
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_5, DOCUMENT_1 );
	}

	@Test
	public void predicateLevelBoost() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath1 = mainIndex.binding().analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = mainIndex.binding().analyzedStringField2.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.phrase().field( absoluteFieldPath1 )
								.matching( PHRASE_1 )
						)
						.should( f.phrase().field( absoluteFieldPath2 )
								.matching( PHRASE_1 )
								.boost( 7 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_5, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.should( f.phrase().field( absoluteFieldPath1 )
								.matching( PHRASE_1 )
								.boost( 39 )
						)
						.should( f.phrase().field( absoluteFieldPath2 )
								.matching( PHRASE_1 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_5 );
	}

	@Test
	public void predicateLevelBoost_withConstantScore() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath1 = mainIndex.binding().analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = mainIndex.binding().analyzedStringField2.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.phrase().field( absoluteFieldPath1 )
								.matching( PHRASE_1 )
								.constantScore().boost( 7 )
						)
						.should( f.phrase().field( absoluteFieldPath2 )
								.matching( PHRASE_1 )
								.constantScore().boost( 39 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_5, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.should( f.phrase().field( absoluteFieldPath1 )
								.matching( PHRASE_1 )
								.constantScore().boost( 39 )
						)
						.should( f.phrase().field( absoluteFieldPath2 )
								.matching( PHRASE_1 )
								.constantScore().boost( 7 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_5 );
	}

	@Test
	public void multiFields() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath1 = mainIndex.binding().analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = mainIndex.binding().analyzedStringField2.relativeFieldName;
		String absoluteFieldPath3 = mainIndex.binding().analyzedStringField3.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery;

		// field(...)

		createQuery = phrase -> scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath1 )
						.matching( phrase )
				)
				.toQuery();

		assertThat( createQuery.apply( PHRASE_1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
		assertThat( createQuery.apply( PHRASE_2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_3 ) )
				.hasNoHits();

		// field(...).field(...)

		createQuery = phrase -> scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath1 )
						.field( absoluteFieldPath2 )
						.matching( phrase )
				)
				.toQuery();

		assertThat( createQuery.apply( PHRASE_1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_3 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );

		// field().fields(...)

		createQuery = phrase -> scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath1 )
						.fields( absoluteFieldPath2, absoluteFieldPath3 )
						.matching( phrase )
				)
				.toQuery();

		assertThat( createQuery.apply( PHRASE_1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_3, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_4, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_3 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );

		// fields(...)

		createQuery = phrase -> scope.query()
				.where( f -> f.phrase()
						.fields( absoluteFieldPath1, absoluteFieldPath2 )
						.matching( phrase )
				)
				.toQuery();

		assertThat( createQuery.apply( PHRASE_1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_5 );
		assertThat( createQuery.apply( PHRASE_3 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	public void error_unknownField() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		Assertions.assertThatThrownBy(
				() -> scope.predicate().phrase().field( "unknown_field" ),
				"phrase() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().phrase()
						.fields( absoluteFieldPath, "unknown_field" ),
				"phrase() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().phrase().field( absoluteFieldPath )
						.field( "unknown_field" ),
				"phrase() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().phrase().field( absoluteFieldPath )
						.fields( "unknown_field" ),
				"phrase() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Test
	public void error_invalidSlop() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		Assertions.assertThatThrownBy(
				() -> scope.predicate().phrase().field( absoluteFieldPath )
						.matching( "foo" ).slop( -1 ),
				"phrase() predicate with negative slop"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid slop" )
				.hasMessageContaining( "must be positive or zero" );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().phrase().field( absoluteFieldPath )
						.matching( "foo" ).slop( Integer.MIN_VALUE ),
				"phrase() predicate with negative slop"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid slop" )
				.hasMessageContaining( "must be positive or zero" );
	}

	@Test
	public void multiIndex_withCompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope(
				compatibleIndex
		);

		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( compatibleIndex.typeName(), COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( rawFieldCompatibleIndex.typeName(), RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer() {
		StubMappingScope scope = mainIndex.createScope( incompatibleAnalyzerIndex );
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		Assertions.assertThatThrownBy(
				() -> {
					scope.query()
							.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1_UNIQUE_TERM ) )
							.toQuery();
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( absoluteFieldPath )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleAnalyzerIndex.name() )
				) )
		;
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_overrideAnalyzer() {
		StubMappingScope scope = mainIndex.createScope( incompatibleAnalyzerIndex );
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1_UNIQUE_TERM )
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
			b.doc( incompatibleAnalyzerIndex.typeName(), INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_searchAnalyzer() {
		StubMappingScope scope = mainIndex.createScope( compatibleSearchAnalyzerIndex );
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1_UNIQUE_TERM ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
			b.doc( compatibleSearchAnalyzerIndex.typeName(), COMPATIBLE_SEARCH_ANALYZER_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_skipAnalysis() {
		StubMappingScope scope = mainIndex.createScope( incompatibleAnalyzerIndex );
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1_UNIQUE_TERM )
						.skipAnalysis() )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
			b.doc( incompatibleAnalyzerIndex.typeName(), INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_incompatibleSearchable() {
		StubMappingScope scope = mainIndex.createScope( unsearchableFieldsIndex );
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		Assertions.assertThatThrownBy( () -> scope.predicate().phrase().field( absoluteFieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( absoluteFieldPath )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), unsearchableFieldsIndex.name() )
				) )
		;
	}

	private void initData() {
		BulkIndexer mainIndexer = mainIndex.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					document.addValue( mainIndex.binding().analyzedStringField1.reference, PHRASE_1_TEXT_EXACT_MATCH );
					document.addValue( mainIndex.binding().analyzedStringFieldWithDslConverter.reference, PHRASE_1_TEXT_EXACT_MATCH );
					document.addValue( mainIndex.binding().whitespaceAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH.toLowerCase( Locale.ROOT ) );
					document.addValue( mainIndex.binding().whitespaceLowercaseAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH.toLowerCase( Locale.ROOT ) );
					document.addValue( mainIndex.binding().whitespaceLowercaseSearchAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH.toLowerCase( Locale.ROOT ) );
					document.addValue( mainIndex.binding().nonAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH );
				} )
				.add( DOCUMENT_2, document -> {
					document.addValue( mainIndex.binding().analyzedStringField1.reference, PHRASE_1_TEXT_SLOP_1_MATCH );
					document.addValue( mainIndex.binding().analyzedStringField2.reference, PHRASE_2_TEXT_EXACT_MATCH );
					document.addValue( mainIndex.binding().whitespaceAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH.toUpperCase( Locale.ROOT ) );
					document.addValue( mainIndex.binding().whitespaceLowercaseAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH.toUpperCase( Locale.ROOT ) );
					document.addValue( mainIndex.binding().whitespaceLowercaseSearchAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH.toUpperCase( Locale.ROOT ) );
					document.addValue( mainIndex.binding().nonAnalyzedField.reference, PHRASE_1_TEXT_SLOP_1_MATCH );
				} )
				.add( DOCUMENT_3, document -> {
					document.addValue( mainIndex.binding().analyzedStringField1.reference, PHRASE_1_TEXT_SLOP_2_MATCH );
					document.addValue( mainIndex.binding().analyzedStringField2.reference, PHRASE_3_TEXT_EXACT_MATCH );
					document.addValue( mainIndex.binding().analyzedStringField3.reference, PHRASE_1_TEXT_EXACT_MATCH );
					document.addValue( mainIndex.binding().whitespaceAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH );
					document.addValue( mainIndex.binding().whitespaceLowercaseAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH );
					document.addValue( mainIndex.binding().whitespaceLowercaseSearchAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH );
					document.addValue( mainIndex.binding().nonAnalyzedField.reference, PHRASE_1_TEXT_SLOP_2_MATCH );
				} )
				.add( DOCUMENT_4, document -> {
					document.addValue( mainIndex.binding().analyzedStringField1.reference, PHRASE_1_TEXT_SLOP_3_MATCH );
					document.addValue( mainIndex.binding().analyzedStringField3.reference, PHRASE_2_TEXT_EXACT_MATCH );
				} )
				.add( DOCUMENT_5, document -> {
					document.addValue( mainIndex.binding().analyzedStringField1.reference, PHRASE_2_TEXT_EXACT_MATCH );
					document.addValue( mainIndex.binding().analyzedStringField2.reference, PHRASE_1_TEXT_EXACT_MATCH );
				} )
				.add( EMPTY, document -> { } );
		BulkIndexer compatibleIndexer = compatibleIndex.bulkIndexer()
				.add( COMPATIBLE_INDEX_DOCUMENT_1, document -> {
					document.addValue( compatibleIndex.binding().analyzedStringField1.reference, PHRASE_1_TEXT_EXACT_MATCH );
				} );
		BulkIndexer rawFieldCompatibleIndexer = rawFieldCompatibleIndex.bulkIndexer()
				.add( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1, document -> {
					document.addValue( rawFieldCompatibleIndex.binding().analyzedStringField1.reference, PHRASE_1_TEXT_EXACT_MATCH );
				} );
		BulkIndexer incompatibleAnalyzerIndexer = incompatibleAnalyzerIndex.bulkIndexer()
				.add( INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1, document -> {
					document.addValue( incompatibleAnalyzerIndex.binding().analyzedStringField1.reference, PHRASE_1_TEXT_EXACT_MATCH );
				} );
		BulkIndexer compatibleSearchAnalyzerIndexer = compatibleSearchAnalyzerIndex.bulkIndexer()
				.add( COMPATIBLE_SEARCH_ANALYZER_INDEX_DOCUMENT_1, document -> {
					document.addValue( compatibleSearchAnalyzerIndex.binding().analyzedStringField1.reference, PHRASE_1_TEXT_EXACT_MATCH );
				} );
		mainIndexer.join(
				compatibleIndexer, rawFieldCompatibleIndexer, compatibleSearchAnalyzerIndexer,
				incompatibleAnalyzerIndexer
		);
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

	private static class IndexBinding {
		final List<ByTypeFieldModel> unsupportedFieldModels = new ArrayList<>();

		final MainFieldModel analyzedStringField1;
		final MainFieldModel analyzedStringField2;
		final MainFieldModel analyzedStringField3;
		final MainFieldModel analyzedStringFieldWithDslConverter;
		final MainFieldModel whitespaceAnalyzedField;
		final MainFieldModel whitespaceLowercaseAnalyzedField;
		final MainFieldModel whitespaceLowercaseSearchAnalyzedField;
		final MainFieldModel nonAnalyzedField;

		IndexBinding(IndexSchemaElement root) {
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
							.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
			)
					.map( root, "analyzedStringWithDslConverter" );
			whitespaceAnalyzedField = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name )
			)
					.map( root, "whitespaceAnalyzed" );
			whitespaceLowercaseAnalyzedField = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
			)
					.map( root, "whitespaceLowercaseAnalyzed" );
			whitespaceLowercaseSearchAnalyzedField = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name )
							.searchAnalyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
			)
					.map( root, "whitespaceLowercaseSearchAnalyzed" );
			nonAnalyzedField = MainFieldModel.mapper( c -> c.asString() )
					.map( root, "nonAnalyzedField" );
		}
	}

	private static class OtherIndexBinding {
		static OtherIndexBinding createCompatible(IndexSchemaElement root) {
			return new OtherIndexBinding(
					MainFieldModel.mapper(
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexBinding createRawFieldCompatible(IndexSchemaElement root) {
			return new OtherIndexBinding(
					MainFieldModel.mapper(
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
									// Using a different DSL converter
									.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexBinding createIncompatibleAnalyzer(IndexSchemaElement root) {
			return new OtherIndexBinding(
					MainFieldModel.mapper(
							// Using a different analyzer
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexBinding createCompatibleSearchAnalyzer(IndexSchemaElement root) {
			return new OtherIndexBinding(
					MainFieldModel.mapper(
							// Using a different analyzer
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
									// Overriding it with a compatible one
									.searchAnalyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexBinding createUnsearchableFieldsIndexBinding(IndexSchemaElement root) {
			return new OtherIndexBinding(
					MainFieldModel.mapper(
							// make the field not searchable
							c -> c.asString().searchable( Searchable.NO )
					)
							.map( root, "analyzedString1" )
			);
		}

		final MainFieldModel analyzedStringField1;

		private OtherIndexBinding(MainFieldModel analyzedStringField1) {
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