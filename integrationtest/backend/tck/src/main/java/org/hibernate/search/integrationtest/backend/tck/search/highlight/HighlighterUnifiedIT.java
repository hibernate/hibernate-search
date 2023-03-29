/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterUnifiedOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.common.SearchException;
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
	public void unifiedMaxAnalyzedOffset() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsHighlighterUnifiedTypeMaxAnalyzedOffsetOnFieldsWithTermVector()
		);
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> h.unified()
						.maxAnalyzedOffset( 1 )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>foo</em> and foo and foo much more times" )
				);

		highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> h.unified()
						.maxAnalyzedOffset( "foo and foo".length() )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>foo</em> and <em>foo</em> and foo much more times" )
				);
	}

	@Test
	public void unifiedMaxAnalyzedOffsetWithTermVectorsNotSupported() {
		assumeFalse(
				TckConfiguration.get().getBackendFeatures()
						.supportsHighlighterUnifiedTypeMaxAnalyzedOffsetOnFieldsWithTermVector()
		);

		assertThatThrownBy(
				() -> index.createScope().query().select(
								f -> f.highlight( "string" )
						)
						.where( f -> f.match().field( "string" ).matching( "foo" ) )
						.highlighter( h -> h.unified().maxAnalyzedOffset( 1 ) )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"unified highlighter does not support the max analyzed offset setting on fields that have non default term vector storage strategy configured",
						"Either use a plain or fast vector highlighters, or do not set this setting"
				);
	}

	@Test
	public void unifiedMaxAnalyzedOffsetWithoutTermVector() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "stringNoTermVector" )
				)
				.where( f -> f.match().field( "stringNoTermVector" ).matching( "boo" ) )
				.highlighter( h -> h.unified()
						.maxAnalyzedOffset( 1 )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>boo</em> and boo and boo much more times" )
				);

		highlights = scope.query().select(
						f -> f.highlight( "stringNoTermVector" )
				)
				.where( f -> f.match().field( "stringNoTermVector" ).matching( "boo" ) )
				.highlighter( h -> h.unified()
						.maxAnalyzedOffset( "boo and boo".length() )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>boo</em> and <em>boo</em> and boo much more times" )
				);
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
