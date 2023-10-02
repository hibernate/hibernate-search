/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

final class HibernateSearchNormalizerCheckingFilter extends TokenFilter {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final char TOKEN_SEPARATOR = ' ';

	private final String normalizerName;

	private final CharTermAttribute termAtt = addAttribute( CharTermAttribute.class );

	private final StringBuilder concatenatedTokenBuilder = new StringBuilder();

	HibernateSearchNormalizerCheckingFilter(TokenStream input, String normalizerName) {
		super( input );
		this.normalizerName = normalizerName;
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null
				&& getClass().equals( obj.getClass() )
				&& normalizerName.equals( ( (HibernateSearchNormalizerCheckingFilter) obj ).normalizerName )
				&& super.equals( obj );
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 31 + normalizerName.hashCode();
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
