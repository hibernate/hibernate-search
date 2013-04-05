/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

/**
 * A filter which will actually insert spaces. Most filters/tokenizers remove them, but for testing it is
 * sometimes better to insert them again ;-)
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public final class InsertWhitespaceFilter extends TokenFilter {

	private TermAttribute termAtt;

	public InsertWhitespaceFilter(TokenStream in) {
		super( in );
		termAtt = addAttribute( TermAttribute.class );
	}

	@Override
	public boolean incrementToken() throws IOException {
		if ( input.incrementToken() ) {
			String value = " " + termAtt.term() + " ";
			termAtt.setTermBuffer( value );
			return true;
		}
		else {
			return false;
		}
	}

}
