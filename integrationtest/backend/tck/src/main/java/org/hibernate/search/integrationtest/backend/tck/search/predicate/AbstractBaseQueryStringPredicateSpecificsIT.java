/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.dsl.CommonQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

abstract class AbstractBaseQueryStringPredicateSpecificsIT<P extends CommonQueryStringPredicateFieldStep<?>> {

	protected static final String DOCUMENT_1 = "document1";
	protected static final String DOCUMENT_2 = "document2";
	protected static final String DOCUMENT_3 = "document3";
	protected static final String DOCUMENT_4 = "document4";
	protected static final String DOCUMENT_5 = "document5";
	protected static final String EMPTY = "empty";

	protected static final String TERM_1 = "word";
	protected static final String TERM_2 = "panda";
	protected static final String TERM_3 = "room";
	protected static final String PHRASE_WITH_TERM_2 = "panda breeding";
	protected static final String PHRASE_WITH_TERM_4 = "elephant john";
	protected static final String PREFIX_FOR_TERM_1_AND_TERM_6 = "wor";
	protected static final String PREFIX_FOR_TERM_6 = "worl";
	protected static final String PREFIX_FOR_TERM_1_AND_TERM_6_DIFFERENT_CASE = "Wor";
	protected static final String PREFIX_FOR_TERM_6_DIFFERENT_CASE = "Worl";
	protected static final String TEXT_TERM_1_AND_TERM_2 = "Here I was, feeding my panda, and the crowd had no word.";
	protected static final String TEXT_TERM_1_AND_TERM_3 = "Without a word, he went out of the room.";
	protected static final String TEXT_TERM_2_IN_PHRASE = "I admired her for her panda breeding expertise.";
	protected static final String TEXT_TERM_4_IN_PHRASE_SLOP_2 = "An elephant ran past John.";
	protected static final String TEXT_TERM_1_EDIT_DISTANCE_1_OR_TERM_6 = "I came to the world in a dumpster.";

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	protected static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );
	protected static final SimpleMappedIndex<IndexSyntaxParsingBinding> indexForSyntaxParsingCheck =
			SimpleMappedIndex.of( IndexSyntaxParsingBinding::new )
					.name( "indexForSyntaxParsingCheck" );

	private static final DataSet dataSet = new DataSet();
	protected static final List<StandardFieldTypeDescriptor<?>> supported =
			FieldTypeDescriptor.getAllStandard().stream()
					// geo point is not supported:
					.filter( Predicate.not( GeoPointFieldTypeDescriptor.INSTANCE::equals ) )
					// strings are tested elsewhere:
					.filter( descriptor -> !String.class.isAssignableFrom( descriptor.getJavaType() ) )
					.collect( Collectors.toList() );
	protected static final SyntaxDataSet syntaxDataSet = new SyntaxDataSet( supported );


	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes( index, indexForSyntaxParsingCheck )
				.setup();

		BulkIndexer indexer = index.bulkIndexer();
		dataSet.contribute( indexer );

		BulkIndexer syntaxIndexer = indexForSyntaxParsingCheck.bulkIndexer();
		syntaxDataSet.contribute( syntaxIndexer );

		indexer.join( syntaxIndexer );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testSimpleQueryString")
	void booleanOperators() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> predicate( f ).field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( TERM_1 + " " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " | " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		assertThatQuery( createQuery.apply( "+" + TERM_1 + " +" + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( createQuery.apply( "-" + TERM_1 + " + " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " + -" + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3844") // Used to throw NPE
	void nonAnalyzedField() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().nonAnalyzedField.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> predicate( f ).field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( TERM_1 + " OR " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " || " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );

		assertThatQuery( createQuery.apply( " +" + TERM_1 + " +" + TERM_2 ) )
				.hasNoHits();

		assertThatQuery( createQuery.apply( "-" + TERM_1 + " +" + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " + -" + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testSimpleQueryString")
	void defaultOperator() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;
		SearchQuery<DocumentReference> query;

		query = scope.query()
				.where( f -> predicate( f ).field( absoluteFieldPath )
						.matching( TERM_1 + " " + TERM_2 ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = scope.query()
				.where( f -> predicate( f ).field( absoluteFieldPath )
						.matching( TERM_1 + " " + TERM_2 )
						.defaultOperator( BooleanOperator.OR ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = scope.query()
				.where( f -> predicate( f ).field( absoluteFieldPath )
						.matching( TERM_1 + " " + TERM_2 )
						.defaultOperator( BooleanOperator.AND ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testSimpleQueryString")
	void phrase() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> predicate( f ).field( absoluteFieldPath ).matching( queryString ) )
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
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testFuzzy")
	void fuzzy() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> predicate( f ).field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( TERM_1 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );

		assertThatQuery( createQuery.apply( TERM_1 + "~1" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );

		assertThatQuery( createQuery.apply( TERM_1 + "~2" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );
	}

	@Test
	void prefix() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> predicate( f ).field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( PREFIX_FOR_TERM_1_AND_TERM_6 + "*" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );

		assertThatQuery( createQuery.apply( PREFIX_FOR_TERM_6 + "*" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_5 );
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3612", "HSEARCH-3845" })
	void prefix_normalizePrefixTerm() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> predicate( f ).field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( PREFIX_FOR_TERM_1_AND_TERM_6_DIFFERENT_CASE + "*" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );

		assertThatQuery( createQuery.apply( PREFIX_FOR_TERM_6_DIFFERENT_CASE + "*" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_5 );
	}

	@ParameterizedTest
	@MethodSource("minimumShouldMatch")
	void minimumShouldMatch(String phrase, int min, String docId, String[] otherDocIds) {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> predicate( f )
						.field( absoluteFieldPath )
						.matching( phrase )
						.minimumShouldMatchNumber( min ) )
				.toQuery();
		if ( docId == null ) {
			assertThatQuery( query )
					.hasNoHits();
		}
		else {
			assertThatQuery( query )
					.hasDocRefHitsAnyOrder( index.typeName(), docId, otherDocIds );
		}
	}

	@ParameterizedTest
	@MethodSource("minimumShouldMatch")
	void minimumShouldMatch_dsl(String phrase, int min, String docId, String[] otherDocIds) {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> predicate( f )
						.field( absoluteFieldPath )
						.matching( phrase )
						.minimumShouldMatch().ifMoreThan( 0 )
						.thenRequireNumber( min )
						.end() )
				.toQuery();
		if ( docId == null ) {
			assertThatQuery( query )
					.hasNoHits();
		}
		else {
			assertThatQuery( query )
					.hasDocRefHitsAnyOrder( index.typeName(), docId, otherDocIds );
		}
	}

	public static List<? extends Arguments> minimumShouldMatch() {
		// "Here I was, feeding my panda, and the crowd had no word."
		return List.of(
				Arguments.of( "feeding my panda", 1, DOCUMENT_1, new String[] { DOCUMENT_3 } ),
				Arguments.of( "feeding my panda", 2, DOCUMENT_1, new String[] { } ),
				Arguments.of( "feeding my panda", 3, DOCUMENT_1, new String[] { } ),
				Arguments.of( "feeding my panda crowd had", 5, DOCUMENT_1, new String[] { } ),
				Arguments.of( "some other panda less matches", 5, null, new String[] { } )
		);
	}

	@ParameterizedTest
	@MethodSource("minimumShouldMatchPercent")
	void minimumShouldMatch_percent(String phrase, int min, String docId, String[] otherDocIds) {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> predicate( f )
						.field( absoluteFieldPath )
						.matching( phrase )
						.minimumShouldMatchPercent( min ) )
				.toQuery();
		if ( docId == null ) {
			assertThatQuery( query )
					.hasNoHits();
		}
		else {
			assertThatQuery( query )
					.hasDocRefHitsAnyOrder( index.typeName(), docId, otherDocIds );
		}
	}

	public static List<? extends Arguments> minimumShouldMatchPercent() {
		// "Here I was, feeding my panda, and the crowd had no word."
		return List.of(
				Arguments.of( "feeding my panda", 34, DOCUMENT_1, new String[] { DOCUMENT_3 } ),
				Arguments.of( "feeding my panda", 67, DOCUMENT_1, new String[] { } ),
				Arguments.of( "feeding my panda", 100, DOCUMENT_1, new String[] { } ),
				Arguments.of( "feeding my panda crowd had", 100, DOCUMENT_1, new String[] { } ),
				Arguments.of( "some other panda less matches", 100, null, new String[] { } )
		);
	}

	@Test
	void incompatibleNestedPaths() {
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

	protected static class IndexBinding {
		final SimpleFieldModel<String> analyzedStringField1;
		final SimpleFieldModel<String> nonAnalyzedField;

		final ObjectFieldBinding nested;

		IndexBinding(IndexSchemaElement root) {
			analyzedStringField1 = SimpleFieldModel.mapperWithOverride( AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			).map( root, "analyzedString1" );
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
			} ).add( DOCUMENT_2, document -> {
				document.addValue( index.binding().nonAnalyzedField.reference, TERM_1 );
				document.addValue( index.binding().analyzedStringField1.reference, TEXT_TERM_1_AND_TERM_3 );
			} ).add( DOCUMENT_3, document -> {
				document.addValue( index.binding().nonAnalyzedField.reference, TERM_2 );
				document.addValue( index.binding().analyzedStringField1.reference, TEXT_TERM_2_IN_PHRASE );
			} ).add( DOCUMENT_4, document -> {
				document.addValue( index.binding().analyzedStringField1.reference, TEXT_TERM_4_IN_PHRASE_SLOP_2 );
			} ).add( DOCUMENT_5, document -> {
				document.addValue( index.binding().analyzedStringField1.reference, TEXT_TERM_1_EDIT_DISTANCE_1_OR_TERM_6 );
			} ).add( EMPTY, document -> {} );
		}
	}

	protected static final class SyntaxDataSet extends AbstractPredicateDataSet {

		private final List<StandardFieldTypeDescriptor<?>> descriptors;

		public SyntaxDataSet(List<StandardFieldTypeDescriptor<?>> descriptors) {
			super( "" );
			this.descriptors = descriptors;
		}

		public void contribute(BulkIndexer indexer) {
			indexer.add( docId( 1 ), document -> {
				for ( StandardFieldTypeDescriptor<?> descriptor : descriptors ) {
					document.addValue( descriptor.getUniqueName(),
							descriptor.getAscendingUniqueTermValues().getSingle().get( 0 ) );
				}
			} ).add( docId( 2 ), document -> {
				for ( StandardFieldTypeDescriptor<?> descriptor : descriptors ) {
					document.addValue( descriptor.getUniqueName(),
							descriptor.getAscendingUniqueTermValues().getSingle().get( 1 ) );
				}
			} ).add( docId( 3 ), document -> {
				for ( StandardFieldTypeDescriptor<?> descriptor : descriptors ) {
					List<?> single = descriptor.getAscendingUniqueTermValues().getSingle();
					document.addValue(
							descriptor.getUniqueName(),
							single.size() < 3 ? null : single.get( 2 )
					);
				}
			} );
		}
	}

	protected static class IndexSyntaxParsingBinding {
		IndexSyntaxParsingBinding(IndexSchemaElement root) {
			for ( StandardFieldTypeDescriptor<?> descriptor : supported ) {
				root.field( descriptor.getUniqueName(), descriptor::configure ).toReference();
				root.field( descriptor.getUniqueName() + 2, descriptor::configure ).toReference();
			}
		}
	}

	protected static <T> Arguments arguments(TckBackendFeatures backendFeatures,
			StandardFieldTypeDescriptor<T> typeDescriptor) {
		List<T> single = typeDescriptor.getAscendingUniqueTermValues().getSingle();
		return Arguments.of(
				typeDescriptor.getUniqueName(),
				escape( backendFeatures.formatForQueryStringPredicate( typeDescriptor, single.get( 0 ) ) ),
				escape( backendFeatures.formatForQueryStringPredicate( typeDescriptor, single.get( 1 ) ) ),
				escape( backendFeatures.formatForQueryStringPredicate( typeDescriptor,
						single.size() < 4 ? null : single.get( 3 ) ) ),
				"not-an-" + typeDescriptor.getUniqueName()
		);
	}

	private static String escape(String value) {
		if ( value == null ) {
			return null;
		}
		value = value.replace( ":", "\\:" );
		value = value.replace( "[", "\\[" );
		value = value.replace( "]", "\\]" );
		value = value.replace( "/", "\\/" );
		value = value.replace( "+", "\\+" );
		value = value.replace( "-", "\\-" );
		if ( value.startsWith( "-" ) || value.startsWith( "+" ) ) {
			value = "\\" + value;
		}
		return value;
	}

	abstract P predicate(SearchPredicateFactory f);
}
