/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFastVectorHighlighterOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterTagSchema;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Test;

public class HighlighterFastVectorIT extends AbstractHighlighterIT {

	@Override
	HighlighterFastVectorHighlighterOptionsStep highlighter(SearchHighlighterFactory factory) {
		return factory.fastVector();
	}

	@Override
	protected List<String> fragmentSizeResult() {
		return Arrays.asList( "Lorem <em>ipsum</em> dolor sit", "Proin nec <em>ipsum</em> ultricies" );
	}

	@Override
	protected List<String> numberOfFragmentsResult() {
		return Arrays.asList( "Lorem <em>ipsum</em> dolor sit amet, consectetur adipiscing elit. Proin nec <em>ipsum</em> ultricies, blandit velit vitae" );
	}

	@Override
	protected List<String> multivaluedFieldResult() {
		return Arrays.asList(
				"fox jumps right over the little lazy <em>dog</em>",
				"This string mentions a <em>dog</em>"
		);
	}

	@Override
	protected List<List<String>> orderByScoreResult() {
		return Arrays.asList(
				Arrays.asList(
						"This <em>string</em> mentions a <em>dog</em>",
						"This <em>string</em> mentions a fox",
						"jumps right over the little lazy <em>dog</em> This"
				)
		);
	}

	@Override
	protected boolean supportsNoMatchSizeOnMultivaluedFields() {
		return TckConfiguration.get().getBackendFeatures()
				.supportsHighlighterFastVectorNoMatchSizeOnMultivaluedFields();
	}

	@Test
	public void boundaryScannerCharsExplicit() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "rock" ) )
				.highlighter( h -> h.fastVector()
						.boundaryScanner()
						.chars()
						.end()
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "Scorpions are a German <em>rock</em> band formed in Hanover in 1965 by guitarist Rudolf Schenker. Since the band's",
								"style has ranged from hard <em>rock</em>, heavy metal and glam metal to soft <em>rock</em>." )
				);
	}

	@Test
	public void boundaryScannerWord() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "rock" ) )
				.highlighter( h -> h.fastVector()
						.boundaryScanner()
						.word()
						.locale( Locale.ENGLISH )
						.end()
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								"Scorpions are a German <em>rock</em> band formed in Hanover in 1965 by guitarist Rudolf Schenker. Since the band's",
								" style has ranged from hard <em>rock</em>, heavy metal and glam metal to soft <em>rock</em>."
						)
				);
	}

	@Test
	public void boundaryScannerWordUsingLambda() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "rock" ) )
				.highlighter( h -> h.fastVector()
						.boundaryScanner( bs -> bs.word().locale( Locale.ENGLISH ) )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								"Scorpions are a German <em>rock</em> band formed in Hanover in 1965 by guitarist Rudolf Schenker. Since the band's",
								" style has ranged from hard <em>rock</em>, heavy metal and glam metal to soft <em>rock</em>."
						)
				);
	}

	@Test
	public void boundaryScannerSentence() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "rock" ) )
				.highlighter( h -> h.fastVector()
						.boundaryScanner()
						.sentence()
						.locale( Locale.ENGLISH )
						.end()
						// limit the fragment size so that the result is sentence-per-highlighted-fragment.
						// trying to go with a lower value, e.g. with 50 would result in 3 fragments, where second sentence
						// gets highlighted two times, once per occurrence :shrug:
						// Result with fragment size  == 50
						//    "Scorpions are a German <em>rock</em> band formed in Hanover in 1965 by guitarist Rudolf Schenker. ",
						//    "Since the band's inception, its musical style has ranged from hard <em>rock</em>, heavy metal and glam metal to soft rock. ",
						//    "Since the band's inception, its musical style has ranged from hard rock, heavy metal and glam metal to soft <em>rock</em>."
						.fragmentSize( "Scorpions are a German rock band formed in Hanover in 1965 by guitarist Rudolf Schenker".length() )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								"Scorpions are a German <em>rock</em> band formed in Hanover in 1965 by guitarist Rudolf Schenker. ",
								"Since the band's inception, its musical style has ranged from hard <em>rock</em>, heavy metal and glam metal to soft <em>rock</em>."
						)
				);
	}

	@Test
	public void styledSchema() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( h2 -> h2.fastVector().tagSchema( HighlighterTagSchema.STYLED ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <em class=\"hlt1\">another</em> value" ),
						Arrays.asList( "some yet <em class=\"hlt1\">another</em> value" )
				);
	}

	/**
	 * Even if we specify a set of tags but at the same time we add STYLED schema it would override the tags and use a
	 * predefined styled schema.
	 */
	@Test
	public void styledSchemaWinsOverTags() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( h2 -> h2.fastVector().tag( "<strong>", "</strong>" ).tagSchema( HighlighterTagSchema.STYLED ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <em class=\"hlt1\">another</em> value" ),
						Arrays.asList( "some yet <em class=\"hlt1\">another</em> value" )
				);
	}

	@Test
	public void tagsWinOverStyledSchema() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( h2 -> h2.fastVector().tagSchema( HighlighterTagSchema.STYLED ).tag( "<strong>", "</strong>" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <strong>another</strong> value" ),
						Arrays.asList( "some yet <strong>another</strong> value" )
				);
	}

	@Test
	public void boundaryCharacters() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "useless" ) )
				.highlighter( h2 -> h2.fastVector()
						.fragmentSize( 20 )
						.boundaryScanner()
						.chars()
						.locale( Locale.ENGLISH )
						.boundaryChars( "-" )
						.boundaryMaxScan( 0 )
						.end()
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						// fragment that we found will just be centered and as we have max-scan set to 0 we won't look for a boundary:
						Arrays.asList( " some <em>useless</em> text i" )
				);

		highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "useless" ) )
				.highlighter( h2 -> h2.fastVector()
						.fragmentSize( 20 )
						.boundaryScanner()
						.chars()
						.locale( Locale.ENGLISH )
						.boundaryChars( "-" )
						.boundaryMaxScan( 25 )
						.end()
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						// now that we have a boundary scan > 0 we are looking for a first match to the left and to the right
						// to find the first occurrence of a boundary char and cut the fragment at it.
						// This will increase the fragment size (compared to the value we've set with `.fragmentSize(..)`)
						Arrays.asList( " to some <em>useless</em> text in between time to see " )
				);
	}

	@Test
	public void boundaryCharactersAsArray() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "useless" ) )
				.highlighter( h2 -> h2.fastVector()
						.fragmentSize( 20 )
						.boundaryScanner()
						.chars()
						.locale( Locale.ENGLISH )
						.boundaryChars( new Character[] { '-' } )
						.boundaryMaxScan( 25 )
						.end()
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( " to some <em>useless</em> text in between time to see " )
				);
	}

	@Test
	public void phraseLimit() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "rock" ) )
				.highlighter( h2 -> h2.fastVector()
						.phraseLimit( 1 )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "Scorpions are a German <em>rock</em> band formed in Hanover in 1965 by guitarist Rudolf Schenker. Since the band's" )
				);
	}

	@Test
	public void multipleDifferentTags() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "multiValuedString" )
				)
				.where( f -> f.bool()
						.must( f.match().field( "multiValuedString" ).matching( "dog" ) )
						.should( f.match().field( "multiValuedString" ).matching( "string" ).boost( 10.0f ) ) )
				.highlighter( h -> highlighter( h ).tags( Arrays.asList( "*", "**" ), Arrays.asList( "*", "**" ) ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Collections.singletonList( Arrays.asList(
								"jumps right over the little lazy *dog*",
								"This **string** mentions a *dog*",
								"This **string** mentions a fox"
						) )
				);
	}

	@Test
	public void multipleStyledTags() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "multiValuedString" )
				)
				.where( f -> f.bool()
						.must( f.match().field( "multiValuedString" ).matching( "dog" ) )
						.should( f.match().field( "multiValuedString" ).matching( "string" ).boost( 10.0f ) ) )
				.highlighter( h -> highlighter( h ).tags( Arrays.asList( "<em class=\"h1\">", "<em class=\"h2\">" ), "</em>" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Collections.singletonList( Arrays.asList(
								"jumps right over the little lazy <em class=\"h1\">dog</em>",
								"This <em class=\"h2\">string</em> mentions a <em class=\"h1\">dog</em>",
								"This <em class=\"h2\">string</em> mentions a fox"
						) )
				);
	}

}
