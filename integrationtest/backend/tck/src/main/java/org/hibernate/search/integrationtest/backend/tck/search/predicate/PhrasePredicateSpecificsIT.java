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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PhrasePredicateSpecificsIT {

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
	private static final String PHRASE_1_TEXT_SLOP_2_MATCH =
			"Once upon a time, there was a quick, sad brown fox in a big house.";
	private static final String PHRASE_1_TEXT_SLOP_3_MATCH = "In the big house, the fox was quick.";
	private static final String PHRASE_1_TEXT_SLOP_NO_MATCH = "Completely unrelated phrase.";

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private static final DataSet dataSet = new DataSet();

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndexes( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		dataSet.contribute( indexer );
		indexer.join();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testPhraseQuery")
	void phrase() {
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		assertThatQuery( index.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	void nonAnalyzedField() {
		String absoluteFieldPath = index.binding().nonAnalyzedField.relativeFieldName;

		assertThatQuery( index.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1 ) ) )
				.hasNoHits();

		assertThatQuery( index.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1_TEXT_EXACT_MATCH ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testPhraseQuery")
	void singleTerm() {
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		assertThatQuery( index.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1_UNIQUE_TERM ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testPhraseQuery")
	void slop() {
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;
		Function<Integer, SearchQueryFinalStep<DocumentReference>> createQuery = slop -> index.query()
				.where( f -> f.phrase().field( absoluteFieldPath ).matching( PHRASE_1 ).slop( slop ) );

		assertThatQuery( createQuery.apply( 0 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( createQuery.apply( 1 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );

		assertThatQuery( createQuery.apply( 2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		assertThatQuery( createQuery.apply( 3 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );

		assertThatQuery( createQuery.apply( 50 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> analyzedStringField1;
		final SimpleFieldModel<String> nonAnalyzedField;

		IndexBinding(IndexSchemaElement root) {
			analyzedStringField1 = SimpleFieldModel.mapperWithOverride( AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) )
					.map( root, "analyzedString1" );
			nonAnalyzedField = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "nonAnalyzedField" );
		}
	}

	private static final class DataSet extends AbstractPredicateDataSet {
		public DataSet() {
			super( null );
		}

		public void contribute(BulkIndexer indexer) {
			indexer.add( DOCUMENT_1, document -> {
				document.addValue( index.binding().analyzedStringField1.reference, PHRASE_1_TEXT_EXACT_MATCH );
				document.addValue( index.binding().nonAnalyzedField.reference, PHRASE_1_TEXT_EXACT_MATCH );
			} )
					.add( DOCUMENT_2, document -> {
						document.addValue( index.binding().analyzedStringField1.reference, PHRASE_1_TEXT_SLOP_1_MATCH );
						document.addValue( index.binding().nonAnalyzedField.reference, PHRASE_1_TEXT_SLOP_1_MATCH );
					} )
					.add( DOCUMENT_3, document -> {
						document.addValue( index.binding().analyzedStringField1.reference, PHRASE_1_TEXT_SLOP_2_MATCH );
						document.addValue( index.binding().nonAnalyzedField.reference, PHRASE_1_TEXT_SLOP_2_MATCH );
					} )
					.add( DOCUMENT_4, document -> {
						document.addValue( index.binding().analyzedStringField1.reference, PHRASE_1_TEXT_SLOP_3_MATCH );
					} )
					.add( DOCUMENT_5, document -> {
						document.addValue( index.binding().analyzedStringField1.reference, PHRASE_1_TEXT_SLOP_NO_MATCH );
					} )
					.add( EMPTY, document -> {} );
		}
	}
}
