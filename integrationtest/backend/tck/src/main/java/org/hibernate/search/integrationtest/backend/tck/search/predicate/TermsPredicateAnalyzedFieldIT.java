/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class TermsPredicateAnalyzedFieldIT {

	private static final String DOC_ID = "my_only_document";
	private static final String[] TOKENS = { "be", "have", "do", "say", "will", "would", "get" };
	private static final String[] NOT_PRESENT_TOKENS = { "go", "make", "can", "time" };

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndexes( index ).setup();
		initData();
	}

	@Test
	void matchingAny_rightTerms() {
		for ( String token : TOKENS ) {
			assertThatQuery( index.query().where( f -> f.terms().field( "analyzedField" ).matchingAny( token ) ) )
					.hasDocRefHitsAnyOrder( index.typeName(), DOC_ID );
		}
	}

	@Test
	void matchingAny_wrongTerms() {
		for ( String token : NOT_PRESENT_TOKENS ) {
			assertThatQuery( index.query().where( f -> f.terms().field( "analyzedField" ).matchingAny( token ) ) )
					.hasNoHits();
		}
	}

	@Test
	void matchingAll_someTerms() {
		assertThatQuery( index.query().where( f -> f.terms().field( "analyzedField" )
				.matchingAll( TOKENS[0], TOKENS[1], TOKENS[3] ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOC_ID );
	}

	@Test
	void matchingAll_allTerms() {
		assertThatQuery( index.query().where( f -> f
				.terms().field( "analyzedField" )
				.matchingAll( TOKENS[0], TOKENS[1], TOKENS[2], TOKENS[3], TOKENS[4], TOKENS[5], TOKENS[6] ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOC_ID );
	}

	@Test
	void matchingAll_oneWrongTerm() {
		assertThatQuery( index.query().where( f -> f
				.terms().field( "analyzedField" )
				.matchingAll( TOKENS[0], TOKENS[1], NOT_PRESENT_TOKENS[1] ) ) )
				.hasNoHits();
	}

	private void initData() {
		String text = String.join( " ", TOKENS );

		index.bulkIndexer()
				.add( DOC_ID, document -> {
					document.addValue( index.binding().analyzed, text );
				} )
				.join();
	}

	public static final class IndexBinding {
		private final IndexFieldReference<String> analyzed;

		public IndexBinding(IndexSchemaElement root) {
			analyzed = root.field(
					"analyzedField",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name )
			).toReference();
		}
	}
}
