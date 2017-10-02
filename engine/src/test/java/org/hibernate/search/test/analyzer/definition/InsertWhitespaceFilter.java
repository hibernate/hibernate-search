/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.definition;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * A filter which will actually insert spaces. Most filters/tokenizers remove them, but for testing it is
 * sometimes better to insert them again ;-)
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public final class InsertWhitespaceFilter extends TokenFilter {

	private final CharTermAttribute termAtt = addAttribute( CharTermAttribute.class );

	public InsertWhitespaceFilter(TokenStream in) {
		super( in );
	}

	@Override
	public boolean incrementToken() throws IOException {
		if ( input.incrementToken() ) {
			final char[] termBuffer = termAtt.buffer();
			final int termBufferLength = termAtt.length();
			final char[] newBuffer = new char[termBufferLength + 2];
			System.arraycopy( termBuffer, 0, newBuffer, 1, termBufferLength );
			newBuffer[0] = ' ';
			newBuffer[newBuffer.length - 1] = ' ';
			termAtt.resizeBuffer( newBuffer.length );
			termAtt.copyBuffer( newBuffer, 0, newBuffer.length );
			return true;
		}
		else {
			return false;
		}
	}

}
