/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.analyzer.solr;

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
