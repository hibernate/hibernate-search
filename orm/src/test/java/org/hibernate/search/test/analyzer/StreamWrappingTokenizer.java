/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * A Tokenizer useful for tests as it will return a specific sequence of tokens,
 * as provided to the constructor.
 * This class must be final as it's a requirement for a Lucene Tokenizer.
 *
 * @author Sanne Grinovero
 */
final class StreamWrappingTokenizer extends Tokenizer {

	private final OffsetAttribute offsetAttribute = addAttribute( OffsetAttribute.class );
	private final CharTermAttribute termAttribute = addAttribute( CharTermAttribute.class );
	private final String[] tokens;
	private int position = 0;

	public StreamWrappingTokenizer(Reader input, String[] tokens) {
		super( input );
		this.tokens = tokens;
	}

	@Override
	public boolean incrementToken() throws IOException {
		if ( position >= tokens.length ) {
			return false;
		}
		else {
			clearAttributes();
			final String token = tokens[position++];
			termAttribute.append( token );
			offsetAttribute.setOffset( 0, token.length() );
			return true;
		}
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		position = 0;
	}

}
