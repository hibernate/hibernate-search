/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterUnifiedOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Test;

public class HighlighterUnifiedIT extends AbstractHighlighterIT {

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
	protected boolean supportsMultipleFragmentsAsSeparateItems() {
		return TckConfiguration.get().getBackendFeatures().supportsHighlighterUnifiedTypeMultipleFragmentsAsSeparateItems();
	}

	@Override
	protected boolean supportsFragmentSize() {
		return TckConfiguration.get().getBackendFeatures().supportsHighlighterUnifiedTypeFragmentSize();
	}

	@Test
	public void boundaryScannerWord() {
		assumeTrue(
				"With Lucene the items will just be in a single string.",
				TckConfiguration.get().getBackendFeatures()
						.supportsHighlighterUnifiedTypeMultipleFragmentsAsSeparateItems()
		);
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
	public void boundaryScannerSentenceExplicit() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsHighlighterUnifiedTypeFragmentSize() &&
						TckConfiguration.get().getBackendFeatures().supportsHighlighterUnifiedTypeMultipleFragmentsAsSeparateItems()
		);
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
