// $Id$
package org.hibernate.search.test.analyzer.solr;

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * A filter which will actually insert spaces. Most filters/tokenizers remove them, but for testing it is
 * sometimes better to insert them again ;-)
 *
 * @author Hardy Ferentschik
 */
public class InsertWhitespaceFilter extends TokenFilter {
	public InsertWhitespaceFilter(TokenStream in) {
		super( in );
	}

	public Token next(final Token reusableToken) throws IOException {
		Token nextToken = input.next( reusableToken );
		if ( nextToken != null ) {
			nextToken.setTermBuffer( " " + nextToken.term() + " " );
			return nextToken;
		}
		else {
			return null;
		}
	}
}