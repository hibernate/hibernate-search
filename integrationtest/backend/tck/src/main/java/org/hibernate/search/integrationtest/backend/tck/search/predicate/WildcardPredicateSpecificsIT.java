/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.dsl.SearchQueryFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class WildcardPredicateSpecificsIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String DOCUMENT_4 = "document4";
	private static final String DOCUMENT_5 = "document5";
	private static final String EMPTY = "empty";

	private static final String PATTERN_1 = "local*n";
	private static final String PATTERN_2 = "inter*on";
	private static final String PATTERN_3 = "la*d";
	private static final String TEXT_MATCHING_PATTERN_1 = "Localization in English is a must-have.";
	private static final String TEXT_MATCHING_PATTERN_2 =
			"Internationalization allows to adapt the application to multiple locales.";
	private static final String TEXT_MATCHING_PATTERN_3 = "A had to call the landlord.";
	private static final String TEXT_MATCHING_PATTERN_2_AND_3 = "I had some interaction with that lad.";

	private static final String TERM_PATTERN_1 = "lOCAl*N";
	private static final String TERM_PATTERN_2 = "IN*oN";
	private static final String TERM_PATTERN_3 = "INteR*oN";
	private static final String TERM_PATTERN_1_EXACT_CASE = "Local*n";
	private static final String TERM_PATTERN_2_EXACT_CASE = "iN*On";
	private static final String TERM_PATTERN_3_EXACT_CASE = "Inter*on";
	private static final String TERM_MATCHING_PATTERN_1 = "Localization";
	private static final String TERM_MATCHING_PATTERN_2 = "iNTroSPEctiOn";
	private static final String TERM_MATCHING_PATTERN_2_AND_3 = "Internationalization";

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private static final DataSet dataSet = new DataSet();

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		dataSet.contribute( indexer );
		indexer.join();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testWildcardQuery")
	void matchSingleToken() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( queryString ) );

		assertThatQuery( createQuery.apply( PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( createQuery.apply( PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_4 );

		assertThatQuery( createQuery.apply( PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3612")
	void normalizeMatchingExpression() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().normalizedField.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( queryString ) );

		assertThatQuery( createQuery.apply( TERM_PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( createQuery.apply( TERM_PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3844") // Used to throw NPE
	void nonAnalyzedField() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().nonAnalyzedField.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( queryString ) );

		assertThatQuery( createQuery.apply( TERM_PATTERN_1 ) )
				.hasNoHits();
		assertThatQuery( createQuery.apply( TERM_PATTERN_1_EXACT_CASE ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( createQuery.apply( TERM_PATTERN_2 ) )
				.hasNoHits();
		assertThatQuery( createQuery.apply( TERM_PATTERN_2_EXACT_CASE ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );

		assertThatQuery( createQuery.apply( TERM_PATTERN_3 ) )
				.hasNoHits();
		assertThatQuery( createQuery.apply( TERM_PATTERN_3_EXACT_CASE ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );
	}

	@Test
	void emptyString() {
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		assertThatQuery( index.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( "" ) ) )
				.hasNoHits();
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> analyzedStringField1;
		final SimpleFieldModel<String> analyzedStringField2;
		final SimpleFieldModel<String> analyzedStringField3;
		final SimpleFieldModel<String> normalizedField;
		final SimpleFieldModel<String> nonAnalyzedField;

		IndexBinding(IndexSchemaElement root) {
			analyzedStringField1 = SimpleFieldModel.mapperWithOverride( AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) )
					.map( root, "analyzedString1" );
			analyzedStringField2 = SimpleFieldModel.mapperWithOverride( AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) )
					.map( root, "analyzedString2" );
			analyzedStringField3 = SimpleFieldModel.mapperWithOverride( AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) )
					.map( root, "analyzedString3" );
			normalizedField = SimpleFieldModel.mapperWithOverride( NormalizedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ) )
					.map( root, "normalized" );
			nonAnalyzedField = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "nonAnalyzed" );
		}
	}

	private static final class DataSet extends AbstractPredicateDataSet {
		public DataSet() {
			super( null );
		}

		public void contribute(BulkIndexer indexer) {
			indexer.add( DOCUMENT_1, document -> {
				document.addValue( index.binding().analyzedStringField1.reference, TEXT_MATCHING_PATTERN_1 );
				document.addValue( index.binding().normalizedField.reference, TERM_MATCHING_PATTERN_1 );
				document.addValue( index.binding().nonAnalyzedField.reference, TERM_MATCHING_PATTERN_1 );
			} )
					.add( DOCUMENT_2, document -> {
						document.addValue( index.binding().analyzedStringField1.reference, TEXT_MATCHING_PATTERN_2 );
						document.addValue( index.binding().normalizedField.reference, TERM_MATCHING_PATTERN_2 );
						document.addValue( index.binding().nonAnalyzedField.reference, TERM_MATCHING_PATTERN_2 );
					} )
					.add( DOCUMENT_3, document -> {
						document.addValue( index.binding().analyzedStringField1.reference, TEXT_MATCHING_PATTERN_3 );
						document.addValue( index.binding().normalizedField.reference, TERM_MATCHING_PATTERN_2_AND_3 );
						document.addValue( index.binding().nonAnalyzedField.reference, TERM_MATCHING_PATTERN_2_AND_3 );
					} )
					.add( DOCUMENT_4, document -> {
						document.addValue( index.binding().analyzedStringField1.reference, TEXT_MATCHING_PATTERN_2_AND_3 );
					} )
					.add( DOCUMENT_5, document -> {
						document.addValue( index.binding().analyzedStringField2.reference, TEXT_MATCHING_PATTERN_1 );
						document.addValue( index.binding().analyzedStringField3.reference, TEXT_MATCHING_PATTERN_3 );
					} )
					.add( EMPTY, document -> {} );
		}
	}
}
