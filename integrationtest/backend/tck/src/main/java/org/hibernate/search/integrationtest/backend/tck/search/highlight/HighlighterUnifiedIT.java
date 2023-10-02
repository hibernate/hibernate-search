/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterUnifiedOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.Test;

class HighlighterUnifiedIT extends AbstractHighlighterIT {

	@Override
	HighlighterUnifiedOptionsStep highlighter(SearchHighlighterFactory factory) {
		return factory.unified();
	}

	@Override
	protected List<String> fragmentSizeResult() {
		return Arrays.asList( "Lorem <em>ipsum</em> dolor sit", "Proin nec <em>ipsum</em> ultricies" );
	}

	@Override
	protected boolean supportsNoMatchSize() {
		return TckConfiguration.get().getBackendFeatures().supportsHighlighterUnifiedTypeNoMatchSize();
	}

	@Override
	protected boolean supportsFragmentSize() {
		return TckConfiguration.get().getBackendFeatures().supportsHighlighterUnifiedTypeFragmentSize();
	}

	@Override
	protected boolean supportsPhraseMatching() {
		return TckConfiguration.get().getBackendFeatures().supportsHighlighterUnifiedPhraseMatching();
	}

	@Test
	void boundaryScannerWord() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" )
		)
				.where( f -> f.match().field( "string" ).matching( "rock" ) )
				.highlighter( h -> h.unified()
						.boundaryScanner()
						.word()
						.end()
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>rock</em>", "<em>rock</em>", "<em>rock</em>" )
				);
	}

	@Test
	void boundaryScannerSentenceExplicit() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsHighlighterUnifiedTypeFragmentSize() );
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" )
		)
				.where( f -> f.match().field( "string" ).matching( "rock" ) )
				.highlighter( h -> h.unified()
						.boundaryScanner()
						.sentence().locale( Locale.ENGLISH ).end()
						.fragmentSize( 150 ) // set the size of fragments so that sentence won't be split into multiple snippets.
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								"Scorpions are a German <em>rock</em> band formed in Hanover in 1965 by guitarist Rudolf Schenker.",
								"Since the band's inception, its musical style has ranged from hard <em>rock</em>, heavy metal and glam metal to soft <em>rock</em>."
						)
				);
	}
}
