/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

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
	protected TokenStreamComponents createComponents(String fieldName) {
		Tokenizer tk = tokenizer.create();
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
