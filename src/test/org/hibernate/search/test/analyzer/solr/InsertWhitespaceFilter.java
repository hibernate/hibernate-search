// $Id: AbstractTestAnalyzer.java 15547 2008-11-11 12:57:47Z hardy.ferentschik $
package org.hibernate.search.test.analyzer.solr;

import java.io.Reader;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.search.Filter;


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