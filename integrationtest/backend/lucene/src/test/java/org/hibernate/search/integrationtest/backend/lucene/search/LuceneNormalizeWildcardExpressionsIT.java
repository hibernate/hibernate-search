/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.search.predicate.WildcardPredicateSpecificsIT;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Extends {@link WildcardPredicateSpecificsIT},
 * verifying the normalization of the terms to match in wildcard/prefix predicates,
 * when an analyzer (not just a normalizer) has been defined on the target field.
 * <p>
 * This case seems to be not already supported for the current version of Elasticsearch server.
 */
@TestForIssue(jiraKey = "HSEARCH-3612")
public class LuceneNormalizeWildcardExpressionsIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String DOCUMENT_4 = "document4";

	// notice the arbitrary cases of the terms:
	private static final String PATTERN_1 = "loCAL*n";
	private static final String PATTERN_2 = "iNtER*On";
	private static final String PATTERN_3 = "lA*d";
	private static final String PATTERN_1_AND_2 = PATTERN_1 + " " + PATTERN_2;

	private static final String TEXT_MATCHING_PATTERN_1 = "Localization in English is a must-have.";
	private static final String TEXT_MATCHING_PATTERN_2 =
			"Internationalization allows to adapt the application to multiple locales.";
	private static final String TEXT_MATCHING_PATTERN_3 = "A had to call the landlord.";
	private static final String TEXT_MATCHING_PATTERN_2_AND_3 = "I had some interaction with that lad.";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
		initData();
	}

	@Test
	public void wildcard_normalizeMatchingExpression() {
		StubMappingScope scope = index.createScope();
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.wildcard().field( "analyzed" ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( createQuery.apply( PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_4 );

		assertThatQuery( createQuery.apply( PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void wildcard_tokenizeMatchingExpression() {
		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.wildcard().field( "analyzed" ).matching( PATTERN_1_AND_2 ) )
				.toQuery();

		// The matching expression is supposed to be normalized only, never tokenized.
		assertThatQuery( query ).hasNoHits();
	}

	private void initData() {
		index.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					document.addValue( index.binding().analyzed, TEXT_MATCHING_PATTERN_1 );
				} )
				.add( DOCUMENT_2, document -> {
					document.addValue( index.binding().analyzed, TEXT_MATCHING_PATTERN_2 );
				} )
				.add( DOCUMENT_3, document -> {
					document.addValue( index.binding().analyzed, TEXT_MATCHING_PATTERN_3 );
				} )
				.add( DOCUMENT_4, document -> {
					document.addValue( index.binding().analyzed, TEXT_MATCHING_PATTERN_2_AND_3 );
				} )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> analyzed;

		IndexBinding(IndexSchemaElement root) {
			analyzed = root
					.field( "analyzed",
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) )
					.toReference();
		}
	}
}
