/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ElasticsearchMatchSearchPredicateIT {

	private static final String TEST_TERM = "ThisWillBeLowercasedByTheNormalizer";

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	void match_skipAnalysis_normalizedStringField() {
		assertThatThrownBy( () -> index.createScope().query()
				.where( f -> f.match().field( "normalizedStringField" ).matching( TEST_TERM ).skipAnalysis() )
				.toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cannot skip analysis on field 'normalizedStringField'",
						"the Elasticsearch backend will always normalize arguments before attempting matches on normalized fields" );
	}

	private void initData() {
		index.bulkIndexer()
				.add( "1", document -> document.addValue( index.binding().normalizedStringField, TEST_TERM ) )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> normalizedStringField;

		IndexBinding(IndexSchemaElement root) {
			normalizedStringField = root.field(
					"normalizedStringField",
					c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name )
			)
					.toReference();
		}
	}
}
