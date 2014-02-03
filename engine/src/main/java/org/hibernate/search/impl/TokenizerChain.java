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
package org.hibernate.search.impl;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;

/**
 * Inspired by Apache Solr's org.apache.solr.analysis.TokenizerChain.TokenizerChain
 */
public final class TokenizerChain extends Analyzer {

	private final CharFilterFactory[] charFilters;
	private final TokenizerFactory tokenizer;
	private final TokenFilterFactory[] filters;

	public TokenizerChain(CharFilterFactory[] charFilters, TokenizerFactory tokenizer, TokenFilterFactory[] filters) {
		this.charFilters = charFilters != null ? charFilters : new CharFilterFactory[0];
		this.tokenizer = tokenizer;
		this.filters = filters != null ? filters : new TokenFilterFactory[0];
	}

	@Override
	public Reader initReader(final String fieldName, final Reader reader) {
		if ( charFilters.length > 0 ) {
			Reader cs = reader;
			for ( CharFilterFactory charFilter : charFilters ) {
				cs = charFilter.create( cs );
			}
			return cs;
		}
		else {
			return reader;
		}
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName, final Reader aReader) {
		Tokenizer tk = tokenizer.create( aReader );
		TokenStream ts = tk;
		for ( TokenFilterFactory filter : filters ) {
			ts = filter.create( ts );
		}
		return new TokenStreamComponents( tk, ts );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "TokenizerChain(" );
		for ( CharFilterFactory filter : charFilters ) {
			sb.append( filter );
			sb.append( ", " );
		}
		sb.append( tokenizer );
		for ( TokenFilterFactory filter : filters ) {
			sb.append( ", " );
			sb.append( filter );
		}
		sb.append( ')' );
		return sb.toString();
	}

}
