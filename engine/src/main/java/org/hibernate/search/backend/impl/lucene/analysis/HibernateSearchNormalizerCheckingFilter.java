/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
final class HibernateSearchNormalizerCheckingFilter extends TokenFilter {

	private static final Log log = LoggerFactory.make();

	private static final char TOKEN_SEPARATOR = ' ';

	private final String normalizerName;

	private final CharTermAttribute termAtt = addAttribute( CharTermAttribute.class );

	private final StringBuilder concatenatedTokenBuilder = new StringBuilder();

	protected HibernateSearchNormalizerCheckingFilter(TokenStream input, String normalizerName) {
		super( input );
		this.normalizerName = normalizerName;
	}

	@Override
	public boolean incrementToken() throws IOException {
		int tokenCount = 0;

		concatenatedTokenBuilder.setLength( 0 );

		while ( input.incrementToken() ) {
			++tokenCount;
			if ( tokenCount > 1 ) {
				concatenatedTokenBuilder.append( TOKEN_SEPARATOR );
			}
			concatenatedTokenBuilder.append( termAtt );
		}

		if ( tokenCount > 1 ) {
			termAtt.setEmpty().append( concatenatedTokenBuilder );
			log.normalizerProducedMultipleTokens( normalizerName, tokenCount );
		}

		return tokenCount > 0;
	}

}
