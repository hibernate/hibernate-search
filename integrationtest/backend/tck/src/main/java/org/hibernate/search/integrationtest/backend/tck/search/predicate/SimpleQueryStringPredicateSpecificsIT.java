/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Collections;
import java.util.EnumSet;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class SimpleQueryStringPredicateSpecificsIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String DOCUMENT_4 = "document4";
	private static final String DOCUMENT_5 = "document5";
	private static final String EMPTY = "empty";

	private static final String TERM_1 = "word";
	private static final String TERM_2 = "panda";
	private static final String TERM_3 = "room";
	private static final String PHRASE_WITH_TERM_2 = "panda breeding";
	private static final String PHRASE_WITH_TERM_4 = "elephant john";
	private static final String PREFIX_FOR_TERM_1_AND_TERM_6 = "wor";
	private static final String PREFIX_FOR_TERM_6 = "worl";
	private static final String PREFIX_FOR_TERM_1_AND_TERM_6_DIFFERENT_CASE = "Wor";
	private static final String PREFIX_FOR_TERM_6_DIFFERENT_CASE = "Worl";
	private static final String TEXT_TERM_1_AND_TERM_2 = "Here I was, feeding my panda, and the crowd had no word.";
	private static final String TEXT_TERM_1_AND_TERM_3 = "Without a word, he went out of the room.";
	private static final String TEXT_TERM_2_IN_PHRASE = "I admired her for her panda breeding expertise.";
	private static final String TEXT_TERM_4_IN_PHRASE_SLOP_2 = "An elephant ran past John.";
	private static final String TEXT_TERM_1_EDIT_DISTANCE_1_OR_TERM_6 = "I came to the world in a dumpster.";

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private static final DataSet dataSet = new DataSet();

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndexes( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		dataSet.contribute( indexer );
		indexer.join();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testSimpleQueryString")
	public void booleanOperators() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( TERM_1 + " " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " | " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " + " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( createQuery.apply( "-" + TERM_1 + " + " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " + -" + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3847")
	public void booleanOperators_flags() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		// Don't use a whitespace here: there's a bug in ES6.2 that leads the "|",
		// when interpreted as an (empty) term, to be turned into a match-no-docs query.
		String orQueryString = TERM_1 + "|" + TERM_2;
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( orQueryString )
				.defaultOperator( BooleanOperator.AND )
				.flags( SimpleQueryFlag.OR ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( orQueryString )
				.defaultOperator( BooleanOperator.AND )
				.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.OR ) ) ) )
				.toQuery() )
				// "OR" disabled: "+" is dropped during analysis and we end up with "term1 + term2", since AND is the default operator
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( orQueryString )
				.defaultOperator( BooleanOperator.AND )
				.flags( Collections.emptySet() ) ) )
				// All flags disabled: operators are dropped during analysis (empty tokens)
				// and we end up with "term1 + term2", since AND is the default operator.
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		String andQueryString = TERM_1 + " + " + TERM_2;
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( andQueryString )
				.defaultOperator( BooleanOperator.OR )
				.flags( SimpleQueryFlag.AND ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( andQueryString )
				.defaultOperator( BooleanOperator.OR )
				.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.AND ) ) ) )
				.toQuery() )
				// "AND" disabled: "+" is dropped during analysis and we end up with "term1 | term2", since OR is the default operator
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( andQueryString )
				.defaultOperator( BooleanOperator.OR )
				.flags( Collections.emptySet() ) ) )
				// All flags disabled: operators are dropped during analysis (empty tokens)
				// and we end up with "term1 | term2", since OR is the default operator.
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		String notQueryString = "-" + TERM_1 + " + " + TERM_2;
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( notQueryString )
				.flags( SimpleQueryFlag.AND, SimpleQueryFlag.NOT ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( notQueryString )
				.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.NOT ) ) ) )
				.toQuery() )
				// "NOT" disabled: "-" is dropped during analysis and we end up with "term1 + term2"
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( notQueryString )
				.flags( Collections.emptySet() ) ) )
				// All flags disabled: operators are dropped during analysis (empty tokens)
				// and we end up with "term1 | term2", since OR is the default operator.
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// Don't use a whitespace here: there's a bug in ES6.2 that leads the "("/")",
		// when interpreted as an (empty) term, to be turned into a match-no-docs query.
		String precedenceQueryString = TERM_2 + "+(" + TERM_1 + "|" + TERM_3 + ")";
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( precedenceQueryString )
				.flags( SimpleQueryFlag.AND, SimpleQueryFlag.OR, SimpleQueryFlag.PRECEDENCE ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( precedenceQueryString )
				.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.PRECEDENCE ) ) ) )
				.toQuery() )
				// "PRECENDENCE" disabled: parentheses are dropped during analysis and we end up with "(term2 + term1) | term3"
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( precedenceQueryString )
				.flags( Collections.emptySet() ) ) )
				// All flags disabled: operators are dropped during analysis (empty tokens)
				// and we end up with "term2 | term1 | term3", since OR is the default operator.
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3844") // Used to throw NPE
	public void nonAnalyzedField() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().nonAnalyzedField.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( TERM_1 + " " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " | " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " + " + TERM_2 ) )
				.hasNoHits();

		assertThatQuery( createQuery.apply( "-" + TERM_1 + " + " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " + -" + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testSimpleQueryString")
	public void defaultOperator() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;
		SearchQuery<DocumentReference> query;

		query = scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( TERM_1 + " " + TERM_2 ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( TERM_1 + " " + TERM_2 )
						.defaultOperator( BooleanOperator.OR ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( TERM_1 + " " + TERM_2 )
						.defaultOperator( BooleanOperator.AND ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testSimpleQueryString")
	public void phrase() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( "\"" + PHRASE_WITH_TERM_2 + "\"" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_3 + " \"" + PHRASE_WITH_TERM_2 + "\"" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );

		// Slop
		assertThatQuery( createQuery.apply( "\"" + PHRASE_WITH_TERM_4 + "\"" ) )
				.hasNoHits();
		assertThatQuery( createQuery.apply( "\"" + PHRASE_WITH_TERM_4 + "\"~1" ) )
				.hasNoHits();
		assertThatQuery( createQuery.apply( "\"" + PHRASE_WITH_TERM_4 + "\"~2" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_4 );
		assertThatQuery( createQuery.apply( "\"" + PHRASE_WITH_TERM_4 + "\"~3" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_4 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3847")
	public void phrase_flag() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( "\"" + PHRASE_WITH_TERM_2 + "\"" )
						.flags( SimpleQueryFlag.PHRASE ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( "\"" + PHRASE_WITH_TERM_2 + "\"" )
						.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.PHRASE ) ) ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3 );

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( "\"" + PHRASE_WITH_TERM_2 + "\"" )
						.flags( Collections.emptySet() ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3 );

		// Slop
		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( "\"" + PHRASE_WITH_TERM_4 + "\"~2" )
						.flags( SimpleQueryFlag.PHRASE, SimpleQueryFlag.NEAR ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_4 );

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( "\"" + PHRASE_WITH_TERM_4 + "\"~2" )
						.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.NEAR ) ) ) )
				.toQuery() )
				.hasNoHits();

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( "\"" + PHRASE_WITH_TERM_4 + "\"~2" )
						.flags( Collections.emptySet() ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_4 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testFuzzy")
	public void fuzzy() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( TERM_1 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );

		assertThatQuery( createQuery.apply( TERM_1 + "~1" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );

		assertThatQuery( createQuery.apply( TERM_1 + "~2" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3847")
	public void fuzzy_flag() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( TERM_1 + "~1" )
						.flags( SimpleQueryFlag.FUZZY ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( TERM_1 + "~1" )
						.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.FUZZY ) ) ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( TERM_1 + "~1" )
						.flags( Collections.emptySet() ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void prefix() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( PREFIX_FOR_TERM_1_AND_TERM_6 + "*" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );

		assertThatQuery( createQuery.apply( PREFIX_FOR_TERM_6 + "*" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3847")
	public void prefix_flag() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( PREFIX_FOR_TERM_1_AND_TERM_6 + "*" )
						.flags( SimpleQueryFlag.PHRASE, SimpleQueryFlag.PREFIX ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( PREFIX_FOR_TERM_1_AND_TERM_6 + "*" )
						.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.PREFIX ) ) ) )
				.toQuery() )
				.hasNoHits();

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( PREFIX_FOR_TERM_1_AND_TERM_6 + "*" )
						.flags( Collections.emptySet() ) ) )
				.hasNoHits();
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3612", "HSEARCH-3845" })
	public void prefix_normalizePrefixTerm() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( PREFIX_FOR_TERM_1_AND_TERM_6_DIFFERENT_CASE + "*" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );

		assertThatQuery( createQuery.apply( PREFIX_FOR_TERM_6_DIFFERENT_CASE + "*" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3847")
	public void whitespace() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().nonAnalyzedField.relativeFieldName;

		String whitespaceQueryString = TERM_1 + " " + TERM_2;
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( whitespaceQueryString )
				.flags( SimpleQueryFlag.WHITESPACE ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( whitespaceQueryString )
				.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.WHITESPACE ) ) ) )
				.toQuery() )
				// "WHITESPACE" disabled: "term1 term2" is interpreted as a single term and cannot be found
				.hasNoHits();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3847")
	public void escape() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		String escapedPrefixQueryString = TERM_1 + "\\*";
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( escapedPrefixQueryString )
				.flags( SimpleQueryFlag.AND, SimpleQueryFlag.NOT, SimpleQueryFlag.ESCAPE ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( escapedPrefixQueryString )
				.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.ESCAPE ) ) ) )
				.toQuery() )
				// "ESCAPE" disabled: "\" is interpreted as a literal and the prefix cannot be found
				.hasNoHits();
	}

	@Test
	public void incompatibleNestedPaths() {
		String fieldInRootPath = index.binding().analyzedStringField1.relativeFieldName;
		String fieldInNestedPath = index.binding().nested.fieldPath();
		assertThatThrownBy( () -> index.createScope()
				.predicate().simpleQueryString().field( fieldInNestedPath ).field( fieldInRootPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid target fields:",
						"fields [" + fieldInNestedPath + ", " + fieldInRootPath +
								"] are in different nested documents (field 'nested' vs. index schema root)",
						"All target fields must be in the same document"
				);
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> analyzedStringField1;
		final SimpleFieldModel<String> nonAnalyzedField;

		final ObjectFieldBinding nested;

		IndexBinding(IndexSchemaElement root) {
			analyzedStringField1 = SimpleFieldModel.mapperWithOverride( AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString1" );
			// A field without any analyzer or normalizer
			nonAnalyzedField = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "nonAnalyzed" );
			nested = ObjectFieldBinding.create( root, null, "nested", ObjectStructure.NESTED );
		}
	}

	static class ObjectFieldBinding {
		final IndexObjectFieldReference reference;
		final String absolutePath;

		final SimpleFieldModel<String> field;

		static ObjectFieldBinding create(IndexSchemaElement parent, String parentAbsolutePath, String relativeFieldName,
				ObjectStructure structure) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			String absolutePath = parentAbsolutePath == null ? relativeFieldName : parentAbsolutePath + "." + relativeFieldName;
			return new ObjectFieldBinding( objectField, absolutePath );
		}

		ObjectFieldBinding(IndexSchemaObjectField objectField, String absolutePath) {
			reference = objectField.toReference();
			this.absolutePath = absolutePath;
			field = SimpleFieldModel.mapper( AnalyzedStringFieldTypeDescriptor.INSTANCE )
					.map( objectField, "field" );
		}

		String fieldPath() {
			return absolutePath + "." + field.relativeFieldName;
		}
	}

	private static final class DataSet extends AbstractPredicateDataSet {
		public DataSet() {
			super( null );
		}

		public void contribute(BulkIndexer indexer) {
			indexer.add( DOCUMENT_1, document -> {
				document.addValue( index.binding().nonAnalyzedField.reference, TEXT_TERM_1_AND_TERM_2 );
				document.addValue( index.binding().analyzedStringField1.reference, TEXT_TERM_1_AND_TERM_2 );
			} )
					.add( DOCUMENT_2, document -> {
						document.addValue( index.binding().nonAnalyzedField.reference, TERM_1 );
						document.addValue( index.binding().analyzedStringField1.reference, TEXT_TERM_1_AND_TERM_3 );
					} )
					.add( DOCUMENT_3, document -> {
						document.addValue( index.binding().nonAnalyzedField.reference, TERM_2 );
						document.addValue( index.binding().analyzedStringField1.reference, TEXT_TERM_2_IN_PHRASE );
					} )
					.add( DOCUMENT_4, document -> {
						document.addValue( index.binding().analyzedStringField1.reference, TEXT_TERM_4_IN_PHRASE_SLOP_2 );
					} )
					.add( DOCUMENT_5, document -> {
						document.addValue( index.binding().analyzedStringField1.reference,
								TEXT_TERM_1_EDIT_DISTANCE_1_OR_TERM_6 );
					} )
					.add( EMPTY, document -> {} );
		}
	}
}
