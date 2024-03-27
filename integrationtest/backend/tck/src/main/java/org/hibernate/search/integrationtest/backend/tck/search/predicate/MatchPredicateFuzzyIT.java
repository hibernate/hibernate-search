/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MatchPredicateFuzzyIT {

	private static final List<StandardFieldTypeDescriptor<?>> unsupportedFieldTypes =
			FieldTypeDescriptor.getAllStandard().stream()
					.filter( fieldType -> !String.class.equals( fieldType.getJavaType() )
							// GeoPoints don't support the match predicate to begin with, let alone fuzzy().
							&& !GeoPointFieldTypeDescriptor.INSTANCE.equals( fieldType ) )
					.collect( Collectors.toList() );

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private static final DataSet dataSet = new DataSet();

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer bulkIndexer = index.bulkIndexer();
		dataSet.contribute( index, bulkIndexer );
		bulkIndexer.join();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testFuzzyQuery")
	void fuzzy() {
		String absoluteFieldPath = index.binding().analyzedStringField.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = text -> index.query()
				.where( f -> f.match()
						.field( absoluteFieldPath )
						.matching( text )
						.fuzzy() );

		// max edit distance = default (2), ignored prefix length = default (0)
		assertThatQuery( createQuery.apply( "another word" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "anther ord" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "ather wd" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "ater w" ) )
				.hasNoHits();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testFuzzyQuery")
	void maxEditDistance() {
		String absoluteFieldPath = index.binding().analyzedStringField.relativeFieldName;
		BiFunction<String, Integer, SearchQueryFinalStep<DocumentReference>> createQuery =
				(text, maxEditDistance) -> index.query()
						.where( f -> f.match()
								.field( absoluteFieldPath )
								.matching( text )
								.fuzzy( maxEditDistance ) );

		// max edit distance = 2
		assertThatQuery( createQuery.apply( "another word", 2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "anther ord", 2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "ather wd", 2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "ater w", 2 ) )
				.hasNoHits();

		// max edit distance = 1
		assertThatQuery( createQuery.apply( "another word", 1 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "anther ord", 1 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "ather wd", 1 ) )
				.hasNoHits();
		assertThatQuery( createQuery.apply( "ater w", 1 ) )
				.hasNoHits();

		// max edit distance = 0
		assertThatQuery( createQuery.apply( "another word", 0 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "anther ord", 0 ) )
				.hasNoHits();
		assertThatQuery( createQuery.apply( "ather wd", 0 ) )
				.hasNoHits();
		assertThatQuery( createQuery.apply( "ater w", 0 ) )
				.hasNoHits();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testFuzzyQuery")
	void exactPrefixLength() {
		String absoluteFieldPath = index.binding().analyzedStringField.relativeFieldName;
		BiFunction<String, Integer, SearchQueryFinalStep<DocumentReference>> createQuery =
				(text, exactPrefixLength) -> index.query()
						.where( f -> f.match()
								.field( absoluteFieldPath )
								.matching( text )
								.fuzzy( 1, exactPrefixLength ) );

		// exact prefix length = 0
		assertThatQuery( createQuery.apply( "another word", 0 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "anther wod", 0 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "aother wrd", 0 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "nother ord", 0 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );

		// exact prefix length = 1
		assertThatQuery( createQuery.apply( "another word", 1 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "anther wod", 1 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "aother wrd", 1 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "nother ord", 1 ) )
				.hasNoHits();

		// exact prefix length = 2
		assertThatQuery( createQuery.apply( "another word", 2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "anther wod", 2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "aother wrd", 2 ) )
				.hasNoHits();
		assertThatQuery( createQuery.apply( "nother ord", 2 ) )
				.hasNoHits();
	}

	@Test
	void normalizedStringField() {
		String absoluteFieldPath = index.binding().normalizedStringField.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery;

		createQuery = param -> index.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( param ).fuzzy() );
		assertThatQuery( createQuery.apply( "Irving" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( createQuery.apply( "Irvin" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( createQuery.apply( "rvin" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( createQuery.apply( "rin" ) )
				.hasNoHits();

		createQuery = param -> index.query()
				.where( f -> f.match().field( absoluteFieldPath )
						.matching( param ).fuzzy( 2, 1 ) );
		assertThatQuery( createQuery.apply( "Irving" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( createQuery.apply( "irving" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( createQuery.apply( "Irvin" ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( createQuery.apply( "rving" ) )
				.hasNoHits();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testFuzzyQueryOnMultipleFields")
	void multipleFields() {
		String absoluteFieldPath1 = index.binding().analyzedStringField.relativeFieldName;
		String absoluteFieldPath2 = index.binding().analyzedString2Field.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery;

		createQuery = param -> index.query()
				.where( f -> f.match().fields( absoluteFieldPath1, absoluteFieldPath2 ).matching( param ).fuzzy() );

		assertThatQuery( createQuery.apply( "word" ) ) // distance 1 from doc1:field2, 0 from doc2:field1
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "wd" ) ) // distance 3 from doc1:field2, 2 from doc2:field1
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		assertThatQuery( createQuery.apply( "worldss" ) ) // distance 2 from doc1:field2, 3 from doc2:field1
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( createQuery.apply( "wl" ) ) // distance 3 from doc1:field2, 3 from doc2:field1
				.hasNoHits();
	}

	@Test
	void unsupportedFieldType() {
		SearchPredicateFactory f = index.createScope().predicate();

		for ( FieldTypeDescriptor<?, ?> fieldType : unsupportedFieldTypes ) {
			SimpleFieldModel<?> fieldModel = index.binding().unsupportedTypeFields.get( fieldType );
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = fieldType.getUniquelyMatchableValues().get( 0 );

			assertThatThrownBy( () -> f.match().field( absoluteFieldPath ).matching( valueToMatch ).fuzzy(),
					"match() predicate with fuzzy() and unsupported type on field " + absoluteFieldPath )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining(
							"Full-text features (analysis, fuzziness) are not supported for fields of this type" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );

			assertThatThrownBy( () -> f.match().field( absoluteFieldPath ).matching( valueToMatch ).fuzzy( 1 ),
					"match() predicate with fuzzy(int) and unsupported type on field " + absoluteFieldPath )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining(
							"Full-text features (analysis, fuzziness) are not supported for fields of this type" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );

			assertThatThrownBy( () -> f.match().field( absoluteFieldPath ).matching( valueToMatch ).fuzzy( 1, 1 ),
					"match() predicate with fuzzy(int, int) and unsupported type on field " + absoluteFieldPath )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining(
							"Full-text features (analysis, fuzziness) are not supported for fields of this type" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );
		}
	}

	@Test
	void invalidMaxEditDistance() {
		SearchPredicateFactory f = index.createScope().predicate();
		String absoluteFieldPath = index.binding().analyzedStringField.relativeFieldName;

		assertThatThrownBy( () -> f.match().field( absoluteFieldPath )
				.matching( "foo" ).fuzzy( 3 ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid maximum edit distance" )
				.hasMessageContaining( "0, 1 or 2" );

		assertThatThrownBy( () -> f.match().field( absoluteFieldPath )
				.matching( "foo" ).fuzzy( -1 ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid maximum edit distance" )
				.hasMessageContaining( "0, 1 or 2" );
	}

	@Test
	void invalidPrefixLength() {
		SearchPredicateFactory f = index.createScope().predicate();
		String absoluteFieldPath = index.binding().analyzedStringField.relativeFieldName;

		assertThatThrownBy( () -> f.match().field( absoluteFieldPath )
				.matching( "foo" ).fuzzy( 1, -1 ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid exact prefix length" )
				.hasMessageContaining( "positive or zero" );
	}

	@Test
	void analyzerOverride() {
		String whitespaceAnalyzedField = index.binding().whitespaceAnalyzedField.relativeFieldName;
		String whitespaceLowercaseAnalyzedField = index.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;
		String whitespaceLowercaseSearchAnalyzedField =
				index.binding().whitespaceLowercaseSearchAnalyzedField.relativeFieldName;

		// Terms are never lower-cased, neither at write nor at query time.
		assertThatQuery( index.query()
				.where( f -> f.match().field( whitespaceAnalyzedField ).matching( "WORD" ).fuzzy() ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 2 ) );

		// Terms are always lower-cased, both at write and at query time.
		assertThatQuery( index.query()
				.where( f -> f.match().field( whitespaceLowercaseAnalyzedField ).matching( "WORD" ).fuzzy() ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ), dataSet.docId( 2 ) );

		// Terms are lower-cased only at query time. Because we are overriding the analyzer in the predicate.
		assertThatQuery( index.query()
				.where( f -> f.match().field( whitespaceAnalyzedField ).matching( "WORD" ).fuzzy()
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );

		// Same here. Terms are lower-cased only at query time. Because we've defined a search analyzer.
		assertThatQuery( index.query()
				.where( f -> f.match().field( whitespaceLowercaseSearchAnalyzedField ).matching( "WORD" ).fuzzy() ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );

		// As for the first query, terms are never lower-cased, neither at write nor at query time.
		// Because even if we've defined a search analyzer, we are overriding it with an analyzer in the predicate,
		// since the overriding takes precedence over the search analyzer.
		assertThatQuery( index.query()
				.where( f -> f.match().field( whitespaceLowercaseSearchAnalyzedField ).matching( "WORD" ).fuzzy()
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 2 ) );
	}

	@Test
	void skipAnalysis() {
		String absoluteFieldPath = index.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;

		assertThatQuery( index.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "word another word" ).fuzzy() ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ), dataSet.docId( 2 ) );

		// ignoring the analyzer means that the parameter of match predicate will not be tokenized
		// so it will not match any token
		assertThatQuery( index.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "word another word" ).fuzzy().skipAnalysis() ) )
				.hasNoHits();

		// to have a match with the skipAnalysis option enabled, we have to pass the parameter as a token is
		assertThatQuery( index.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "word" ).fuzzy().skipAnalysis() ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ), dataSet.docId( 2 ) );
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType unsupportedTypeFields;

		private final SimpleFieldModel<String> analyzedStringField;
		private final SimpleFieldModel<String> analyzedString2Field;
		private final SimpleFieldModel<String> normalizedStringField;
		private final SimpleFieldModel<String> whitespaceAnalyzedField;
		private final SimpleFieldModel<String> whitespaceLowercaseAnalyzedField;
		private final SimpleFieldModel<String> whitespaceLowercaseSearchAnalyzedField;


		IndexBinding(IndexSchemaElement root) {
			unsupportedTypeFields = SimpleFieldModelsByType.mapAll(
					unsupportedFieldTypes,
					root, "unsupported_"
			);
			analyzedStringField = SimpleFieldModel.mapperWithOverride(
					AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString" );
			analyzedString2Field = SimpleFieldModel.mapperWithOverride(
					AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString2" );
			normalizedStringField = SimpleFieldModel.mapperWithOverride(
					NormalizedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name )
			)
					.map( root, "normalizedString" );
			whitespaceAnalyzedField = SimpleFieldModel.mapperWithOverride(
					AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name )
			)
					.map( root, "whitespaceAnalyzed" );
			whitespaceLowercaseAnalyzedField = SimpleFieldModel.mapperWithOverride(
					AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
			)
					.map( root, "whitespaceLowercaseAnalyzed" );
			whitespaceLowercaseSearchAnalyzedField = SimpleFieldModel.mapperWithOverride(
					AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name )
							.searchAnalyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
			)
					.map( root, "whitespaceLowercaseSearchAnalyzed" );
		}
	}

	public static final class DataSet extends AbstractPredicateDataSet {
		private final List<String> analyzedStringFieldValues = CollectionHelper.asImmutableList(
				"quick brown fox", "another word", "a"
		);
		private final List<String> analyzedString2FieldValues = CollectionHelper.asImmutableList(
				"another world", "blue whale", "the"
		);
		private final List<String> normalizedStringFieldValues = CollectionHelper.asImmutableList(
				"Irving", "Auster", "Coe"
		);
		private final List<String> whitespaceAnalyzedFieldValues = CollectionHelper.asImmutableList(
				"brave new world", "BRAVE NEW WORLD", "BRave NeW WoRlD"
		);
		private final List<String> whitespaceLowercaseAnalyzedFieldValues = CollectionHelper.asImmutableList(
				"brave new world", "BRAVE NEW WORLD", "BRave NeW WoRlD"
		);
		private final List<String> whitespaceLowercaseSearchAnalyzedFieldValues = CollectionHelper.asImmutableList(
				"brave new world", "BRAVE NEW WORLD", "BRave NeW WoRlD"
		);
		private final List<String> ngramSearchAnalyzedFieldValues = CollectionHelper.asImmutableList(
				"brave new world", "BRAVE NEW WORLD", "BRave NeW WoRlD"
		);

		public DataSet() {
			super( null );
		}

		public void contribute(SimpleMappedIndex<IndexBinding> index, BulkIndexer indexer) {
			indexer.add( docId( 0 ), routingKey,
					document -> initDocument( index, document, 0 ) );
			indexer.add( docId( 1 ), routingKey,
					document -> initDocument( index, document, 1 ) );
			indexer.add( docId( 2 ), routingKey,
					document -> initDocument( index, document, 2 ) );
		}

		private void initDocument(SimpleMappedIndex<IndexBinding> index, DocumentElement document,
				int docOrdinal) {
			IndexBinding binding = index.binding();
			document.addValue( binding.analyzedStringField.reference,
					analyzedStringFieldValues.get( docOrdinal ) );
			document.addValue( binding.analyzedString2Field.reference,
					analyzedString2FieldValues.get( docOrdinal ) );
			document.addValue( binding.normalizedStringField.reference,
					normalizedStringFieldValues.get( docOrdinal ) );
			document.addValue( binding.whitespaceAnalyzedField.reference,
					whitespaceAnalyzedFieldValues.get( docOrdinal ) );
			document.addValue( binding.whitespaceLowercaseAnalyzedField.reference,
					whitespaceLowercaseAnalyzedFieldValues.get( docOrdinal ) );
			document.addValue( binding.whitespaceLowercaseSearchAnalyzedField.reference,
					whitespaceLowercaseSearchAnalyzedFieldValues.get( docOrdinal ) );
		}
	}
}
