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
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.OverrideAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SimpleQueryStringSearchPredicateIT {

	private static final String CONFIGURATION_ID = "analysis-override";

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

	private static final String TERM_1 = "word";
	private static final String TERM_2 = "panda";
	private static final String TERM_3 = "room";
	private static final String TERM_4 = "elephant john";
	private static final String TERM_5 = "crowd";
	private static final String PHRASE_WITH_TERM_2 = "panda breeding";
	private static final String PHRASE_WITH_TERM_4 = "elephant john";
	private static final String TEXT_TERM_1_AND_TERM_2 = "Here I was, feeding my panda, and the crowd had no word.";
	private static final String TEXT_TERM_1_AND_TERM_3 = "Without a word, he went out of the room.";
	private static final String TEXT_TERM_2_IN_PHRASE = "I admired her for her panda breeding expertise.";
	private static final String TEXT_TERM_4_IN_PHRASE_SLOP_2 = "An elephant ran past John.";
	private static final String TEXT_TERM_1_EDIT_DISTANCE_1 = "I came to the world in a dumpster.";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";
	private static final String INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 = "incompatible_analyzer_1";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

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
		setupHelper.withConfiguration( CONFIGURATION_ID )
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
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testSimpleQueryString")
	public void simpleQueryString() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThat( createQuery.apply( TERM_1 + " " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		assertThat( createQuery.apply( TERM_1 + " | " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		assertThat( createQuery.apply( TERM_1 + " + " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		assertThat( createQuery.apply( "-" + TERM_1 + " + " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		assertThat( createQuery.apply( TERM_1 + " + -" + TERM_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
	}

	@Test
	public void simpleQueryString_unsearchable() {
		StubMappingScope scope = unsearchableFieldsIndexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException( () ->
				scope.predicate().simpleQueryString().onField( absoluteFieldPath )
		).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "is not searchable" )
				.hasMessageContaining( "Make sure the field is marked as searchable" )
				.hasMessageContaining( absoluteFieldPath );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testSimpleQueryString")
	public void withAndAsDefaultOperator() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;
		SearchQuery<DocumentReference> query;

		query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath )
						.matching( TERM_1 + " " + TERM_2 ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath )
						.matching( TERM_1 + " " + TERM_2 )
						.withAndAsDefaultOperator() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	/**
	 * Check that a simple query string predicate can be used on a field that has a DSL converter.
	 * The DSL converter should be ignored, and there shouldn't be any exception thrown
	 * (the field should be considered as a text field).
	 */
	@Test
	public void withDslConverter() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringFieldWithDslConverter.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath ).matching( TERM_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2700")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testEmptyQueryString")
	public void emptyStringBeforeAnalysis() {
		StubMappingScope scope = indexManager.createScope();
		MainFieldModel fieldModel = indexMapping.analyzedStringField1;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( fieldModel.relativeFieldName ).matching( "" ) )
				.toQuery();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2700")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testBlankQueryString")
	public void blankStringBeforeAnalysis() {
		StubMappingScope scope = indexManager.createScope();
		MainFieldModel fieldModel = indexMapping.analyzedStringField1;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( fieldModel.relativeFieldName ).matching( "   " ) )
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
				.predicate( f -> f.simpleQueryString().onField( fieldModel.relativeFieldName ).matching( "the a" ) )
				.toQuery();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testAnalyzer")
	public void analyzerOverride() {
		StubMappingScope scope = indexManager.createScope();

		String whitespaceAnalyzedField = indexMapping.whitespaceAnalyzedField.relativeFieldName;
		String whitespaceLowercaseAnalyzedField = indexMapping.whitespaceLowercaseAnalyzedField.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( whitespaceAnalyzedField ).matching( "HERE | PANDA" ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( whitespaceLowercaseAnalyzedField ).matching( "HERE | PANDA" ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( whitespaceAnalyzedField ).matching( "HERE | PANDA" )
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
				.predicate( f -> f.simpleQueryString().onField( whitespaceAnalyzedField ).matching( "HERE | PANDA" )
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
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath ).matching( "HERE | PANDA" ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// ignoring the analyzer means that the parameter of match predicate will not be tokenized
		// so it will not match any token
		query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath ).matching( "HERE | PANDA" ).skipAnalysis() )
				.toQuery();

		assertThat( query )
				.hasNoHits();

		// to have a match with the skipAnalysis option enabled, we have to pass the parameter as a token is
		query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath ).matching( "here" ).skipAnalysis() )
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
					"simpleQueryString() predicate with unsupported type on field " + absoluteFieldPath,
					() -> scope.predicate().simpleQueryString().onField( absoluteFieldPath )
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
	@TestForIssue(jiraKey = "HSEARCH-2700")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testNullQueryString")
	public void error_null() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				"simpleQueryString() predicate with null value to match",
				() -> scope.predicate().simpleQueryString().onField( absoluteFieldPath ).matching( null )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid simple query string" )
				.hasMessageContaining( "must be non-null" )
				.hasMessageContaining( absoluteFieldPath );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testSimpleQueryString")
	public void phrase() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThat( createQuery.apply( "\"" + PHRASE_WITH_TERM_2 + "\"" ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		assertThat( createQuery.apply( TERM_3 + " \"" + PHRASE_WITH_TERM_2 + "\"" ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

		// Slop
		assertThat( createQuery.apply( "\"" + PHRASE_WITH_TERM_4 + "\"" ) )
				.hasNoHits();
		assertThat( createQuery.apply( "\"" + PHRASE_WITH_TERM_4 + "\"~1" ) )
				.hasNoHits();
		assertThat( createQuery.apply( "\"" + PHRASE_WITH_TERM_4 + "\"~2" ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_4 );
		assertThat( createQuery.apply( "\"" + PHRASE_WITH_TERM_4 + "\"~3" ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_4 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testBoost")
	public void fieldLevelBoost() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;
		SearchQuery<DocumentReference> query;

		query = scope.query()
				.predicate( f -> f.simpleQueryString()
						.onField( absoluteFieldPath1 ).boostedTo( 5f )
						.orField( absoluteFieldPath2 )
						.matching( TERM_3 )
				)
				.sort( f -> f.byScore() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_1 );

		query = scope.query()
				.predicate( f -> f.simpleQueryString()
						.onField( absoluteFieldPath1 )
						.orField( absoluteFieldPath2 ).boostedTo( 5f )
						.matching( TERM_3 )
				)
				.sort( f -> f.byScore() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void predicateLevelBoost() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						.should( f.simpleQueryString().onField( absoluteFieldPath1 )
								.matching( TERM_3 )
						)
						.should( f.simpleQueryString().onField( absoluteFieldPath2 )
								.matching( TERM_3 )
								.boostedTo( 7 )
						)
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		query = scope.query()
				.predicate( f -> f.bool()
						.should( f.simpleQueryString().onField( absoluteFieldPath1 )
								.matching( TERM_3 )
								.boostedTo( 39 )
						)
						.should( f.simpleQueryString().onField( absoluteFieldPath2 )
								.matching( TERM_3 )
						)
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	public void predicateLevelBoost_withConstantScore() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						.should( f.simpleQueryString().onField( absoluteFieldPath1 )
								.matching( TERM_3 )
								.withConstantScore().boostedTo( 7 )
						)
						.should( f.simpleQueryString().onField( absoluteFieldPath2 )
								.matching( TERM_3 )
								.withConstantScore().boostedTo( 39 )
						)
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		query = scope.query()
				.predicate( f -> f.bool()
						.should( f.simpleQueryString().onField( absoluteFieldPath1 )
								.matching( TERM_3 )
								.withConstantScore().boostedTo( 39 )
						)
						.should( f.simpleQueryString().onField( absoluteFieldPath2 )
								.matching( TERM_3 )
								.withConstantScore().boostedTo( 7 )
						)
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testFuzzy")
	public void fuzzy() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThat( createQuery.apply( TERM_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		assertThat( createQuery.apply( TERM_1 + "~1" ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );

		assertThat( createQuery.apply( TERM_1 + "~2" ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );
	}

	@Test
	public void multiFields() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;
		String absoluteFieldPath3 = indexMapping.analyzedStringField3.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery;

		// onField(...)

		createQuery = query -> scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath1 )
						.matching( query )
				)
				.toQuery();

		assertThat( createQuery.apply( TERM_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		assertThat( createQuery.apply( TERM_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
		assertThat( createQuery.apply( TERM_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// onField(...).orField(...)

		createQuery = query -> scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath1 )
						.orField( absoluteFieldPath2 )
						.matching( query )
				)
				.toQuery();

		assertThat( createQuery.apply( TERM_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		assertThat( createQuery.apply( TERM_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3, DOCUMENT_4 );
		assertThat( createQuery.apply( TERM_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		// onField().orFields(...)

		createQuery = query -> scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath1 )
						.orFields( absoluteFieldPath2, absoluteFieldPath3 )
						.matching( query )
				)
				.toQuery();

		assertThat( createQuery.apply( TERM_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );
		assertThat( createQuery.apply( TERM_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3, DOCUMENT_4, DOCUMENT_5 );
		assertThat( createQuery.apply( TERM_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );

		// onFields(...)

		createQuery = query -> scope.query()
				.predicate( f -> f.simpleQueryString().onFields( absoluteFieldPath1, absoluteFieldPath2 )
						.matching( query )
				)
				.toQuery();

		assertThat( createQuery.apply( TERM_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		assertThat( createQuery.apply( TERM_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3, DOCUMENT_4 );
		assertThat( createQuery.apply( TERM_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void error_unknownField() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				"simpleQueryString() predicate with unknown field",
				() -> scope.predicate().simpleQueryString().onField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"simpleQueryString() predicate with unknown field",
				() -> scope.predicate().simpleQueryString()
						.onFields( absoluteFieldPath, "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"simpleQueryString() predicate with unknown field",
				() -> scope.predicate().simpleQueryString().onField( absoluteFieldPath )
						.orField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"simpleQueryString() predicate with unknown field",
				() -> scope.predicate().simpleQueryString().onField( absoluteFieldPath )
						.orFields( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Test
	public void multiIndex_withCompatibleIndexManager() {
		StubMappingScope scope = indexManager.createScope(
				compatibleIndexManager
		);
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath ).matching( TERM_1 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
			b.doc( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager() {
		StubMappingScope scope = indexManager.createScope( rawFieldCompatibleIndexManager );
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath ).matching( TERM_1 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
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
							.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath ).matching( TERM_5 ) )
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
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath ).matching( TERM_5 )
						.analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1 );
			b.doc( INCOMPATIBLE_ANALYZER_INDEX_NAME, INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_skipAnalysis() {
		StubMappingScope scope = indexManager.createScope( incompatibleAnalyzerIndexManager );
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.simpleQueryString().onField( absoluteFieldPath ).matching( TERM_5 )
						.skipAnalysis() )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1 );
			b.doc( INCOMPATIBLE_ANALYZER_INDEX_NAME, INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_incompatibleSearchable() {
		StubMappingScope scope = indexManager.createScope( unsearchableFieldsIndexManager );
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException( () -> scope.predicate().simpleQueryString().onField( absoluteFieldPath ) )
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
			document.addValue( indexMapping.analyzedStringField1.reference, TEXT_TERM_1_AND_TERM_2 );
			document.addValue( indexMapping.analyzedStringFieldWithDslConverter.reference, TEXT_TERM_1_AND_TERM_2 );
			document.addValue( indexMapping.analyzedStringField2.reference, TEXT_TERM_1_AND_TERM_3 );
			document.addValue( indexMapping.analyzedStringField3.reference, TERM_4 );
			document.addValue( indexMapping.whitespaceAnalyzedField.reference, TEXT_TERM_1_AND_TERM_2.toLowerCase( Locale.ROOT ) );
			document.addValue( indexMapping.whitespaceLowercaseAnalyzedField.reference, TEXT_TERM_1_AND_TERM_2.toLowerCase( Locale.ROOT ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, TEXT_TERM_1_AND_TERM_3 );
			document.addValue( indexMapping.whitespaceAnalyzedField.reference, TEXT_TERM_1_AND_TERM_2.toUpperCase( Locale.ROOT ) );
			document.addValue( indexMapping.whitespaceLowercaseAnalyzedField.reference, TEXT_TERM_1_AND_TERM_2.toUpperCase( Locale.ROOT ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, TEXT_TERM_2_IN_PHRASE );
			document.addValue( indexMapping.whitespaceAnalyzedField.reference, TEXT_TERM_1_AND_TERM_2 );
			document.addValue( indexMapping.whitespaceLowercaseAnalyzedField.reference, TEXT_TERM_1_AND_TERM_2 );
		} );
		workPlan.add( referenceProvider( DOCUMENT_4 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, TEXT_TERM_4_IN_PHRASE_SLOP_2 );
			document.addValue( indexMapping.analyzedStringField2.reference, TEXT_TERM_2_IN_PHRASE );
		} );
		workPlan.add( referenceProvider( DOCUMENT_5 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, TEXT_TERM_1_EDIT_DISTANCE_1 );
			document.addValue( indexMapping.analyzedStringField3.reference, TEXT_TERM_2_IN_PHRASE );
			document.addValue( indexMapping.analyzedStringField3.reference, TEXT_TERM_1_AND_TERM_3 );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> {
		} );
		workPlan.execute().join();

		workPlan = compatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			document.addValue( compatibleIndexMapping.analyzedStringField1.reference, TEXT_TERM_1_AND_TERM_2 );
		} );
		workPlan.execute().join();

		workPlan = rawFieldCompatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			document.addValue( rawFieldCompatibleIndexMapping.analyzedStringField1.reference, TEXT_TERM_1_AND_TERM_2 );
		} );
		workPlan.execute().join();
		workPlan = incompatibleAnalyzerIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 ), document -> {
			document.addValue( incompatibleAnalyzerIndexMapping.analyzedStringField1.reference, TEXT_TERM_1_AND_TERM_2 );
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
					.mapMultiValued( root, "analyzedString3" );
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
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, String>> configuration) {
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