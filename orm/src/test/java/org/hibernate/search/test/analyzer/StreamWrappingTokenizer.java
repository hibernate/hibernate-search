/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
