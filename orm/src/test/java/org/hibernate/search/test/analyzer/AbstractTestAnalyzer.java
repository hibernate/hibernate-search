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
package org.hibernate.search.test.analyzer;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

/**
 * @author Emmanuel Bernard
 */
public abstract class AbstractTestAnalyzer extends Analyzer {

	protected abstract String[] getTokens();

	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		return new InternalTokenStream();
	}

	private class InternalTokenStream extends TokenStream {
		private int position;
		private TermAttribute termAttribute;

		public InternalTokenStream() {
			super();
			termAttribute = addAttribute( TermAttribute.class );
		}

		@Override
		public boolean incrementToken() throws IOException {
			if ( position >= getTokens().length ) {
				return false;
			}
			else {
				termAttribute.setTermBuffer( getTokens()[position++] );
				return true;
			}
		}
	}
}
