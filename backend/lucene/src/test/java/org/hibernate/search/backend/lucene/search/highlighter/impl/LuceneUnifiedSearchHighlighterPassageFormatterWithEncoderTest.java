/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.highlighter.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.uhighlight.Passage;

public class LuceneUnifiedSearchHighlighterPassageFormatterWithEncoderTest {

	private LuceneUnifiedSearchHighlighter.PassageFormatterWithEncoder formatter =
			new LuceneUnifiedSearchHighlighter.PassageFormatterWithEncoder(
					"<em>", "</em>", new DefaultEncoder() );

	@Test
	public void simple() {
		String text = "The quick brown fox jumps right over the little lazy dog";

		Passage[] passages = new Passage[1];
		Passage passage = new Passage();
		passages[0] = passage;
		passage.setStartOffset( 0 );
		passage.setEndOffset( text.length() );

		int startOffset = text.indexOf( "dog" );
		int endOffset = startOffset + "dog".length();
		passage.addMatch( startOffset, endOffset, null, 1 );

		assertThat( formatter.format( passages, text ) )
				.extracting( LuceneUnifiedSearchHighlighter.TextFragment::highlightedText )
				.containsOnly( "The quick brown fox jumps right over the little lazy <em>dog</em>" );
	}

	@Test
	public void multiplePassages() {
		String text = "The quick brown fox jumps right over the little lazy dog. The five boxing wizards jump quickly";

		Passage[] passages = new Passage[2];
		Passage passage = new Passage();
		passages[0] = passage;
		passage.setStartOffset( 0 );
		passage.setEndOffset( text.indexOf( '.' ) );

		int startOffset = text.indexOf( "dog" );
		int endOffset = startOffset + "dog".length();
		passage.addMatch( startOffset, endOffset, null, 1 );

		passage = new Passage();
		passages[1] = passage;
		passage.setStartOffset( text.indexOf( ". " ) + ". ".length() );
		passage.setEndOffset( text.length() );

		startOffset = text.indexOf( "wizards" );
		endOffset = startOffset + "wizards".length();
		passage.addMatch( startOffset, endOffset, null, 1 );

		assertThat( formatter.format( passages, text ) )
				.extracting( LuceneUnifiedSearchHighlighter.TextFragment::highlightedText )
				.containsOnly(
						"The quick brown fox jumps right over the little lazy <em>dog</em>",
						"The five boxing <em>wizards</em> jump quickly"
				);
	}

	@Test
	public void overlappingPassagesInFormatterFullyIncluded() {
		String text = "The quick brown fox jumps right over the little lazy dog";

		Passage[] passages = new Passage[1];
		Passage passage = new Passage();
		passages[0] = passage;
		passage.setStartOffset( 0 );
		passage.setEndOffset( text.length() );

		int startOffset = text.indexOf( "quick brown" );
		int endOffset = startOffset + "quick brown".length();
		passage.addMatch( startOffset, endOffset, null, 1 );

		startOffset = text.indexOf( "brown" );
		endOffset = startOffset + "brown".length();
		passage.addMatch( startOffset, endOffset, null, 1 );

		// just another match no overlapping:
		startOffset = text.indexOf( "dog" );
		endOffset = startOffset + "dog".length();
		passage.addMatch( startOffset, endOffset, null, 1 );

		assertThat( formatter.format( passages, text ) )
				.extracting( LuceneUnifiedSearchHighlighter.TextFragment::highlightedText )
				.containsOnly( "The <em>quick brown</em> fox jumps right over the little lazy <em>dog</em>" );
	}

	@Test
	public void overlappingPassagesInFormatterPartialOverlapping() {
		String text = "The quick brown fox jumps right over the little lazy dog";

		Passage[] passages = new Passage[1];
		Passage passage = new Passage();
		passages[0] = passage;
		passage.setStartOffset( 0 );
		passage.setEndOffset( text.length() );

		int startOffset = text.indexOf( "quick brown" );
		int endOffset = startOffset + "quick brown".length();
		passage.addMatch( startOffset, endOffset, null, 1 );

		startOffset = text.indexOf( "brown fox" );
		endOffset = startOffset + "brown fox".length();
		passage.addMatch( startOffset, endOffset, null, 1 );

		assertThat( formatter.format( passages, text ) )
				.extracting( LuceneUnifiedSearchHighlighter.TextFragment::highlightedText )
				.containsOnly( "The <em>quick brown fox</em> jumps right over the little lazy dog" );
	}
}
