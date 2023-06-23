/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LuceneMatchSearchPredicateIT {

	private static final String TEST_TERM = "ThisWillBeLowercasedByTheNormalizer";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
		initData();
	}

	@Test
	public void match_skipAnalysis_normalizedStringField() {
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "normalizedStringField" ).matching( TEST_TERM ) )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsAnyOrder( index.typeName(), "1" );

		query = index.createScope().query()
				.where( f -> f.match().field( "normalizedStringField" ).matching( TEST_TERM ).skipAnalysis() )
				.toQuery();

		assertThatQuery( query ).hasNoHits();
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
