/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFragmenter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterPlainOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.Test;

class HighlighterPlainIT extends AbstractHighlighterIT {

	@Override
	HighlighterPlainOptionsStep highlighter(SearchHighlighterFactory factory) {
		return factory.plain();
	}

	@Override
	protected List<String> fragmentSizeResult() {
		return Arrays.asList( "Lorem <em>ipsum</em> dolor", " <em>ipsum</em> ultricies" );
	}

	@Override
	protected List<String> numberOfFragmentsResult() {
		return Arrays.asList(
				"Lorem <em>ipsum</em> dolor sit amet, consectetur adipiscing elit. Proin nec <em>ipsum</em> ultricies, blandit velit"
		);
	}

	@Override
	protected boolean supportsOrderByScoreMultivaluedField() {
		return TckConfiguration.get().getBackendFeatures().supportsHighlighterPlainOrderByScoreMultivaluedField();
	}

	@Override
	protected boolean supportsPhraseMatching() {
		return false;
	}

	@Test
	void plainFragmenterSimple() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" )
		)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> h.plain()
						.fragmenter( HighlighterFragmenter.SIMPLE )
						.fragmentSize( 15 )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>foo</em> and <em>foo</em>", " and <em>foo</em> much more" )
				);
	}

	@Test
	void plainFragmenterSpan() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" )
		)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> h.plain()
						.fragmenter( HighlighterFragmenter.SPAN )
						.fragmentSize( 15 )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>foo</em> and <em>foo</em>", " and <em>foo</em> much more times" )
				);
	}
}
