/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.impl;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharFilterFactory;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenizerFactory;

/**
 * Inspired by Apache Solr's org.apache.solr.analysis.TokenizerChain.TokenizerChain
 */
public final class TokenizerChain extends Analyzer {

	private final CharFilterFactory[] charFilters;
	private final TokenizerFactory tokenizer;
	private final TokenFilterFactory[] filters;

	TokenizerChain(CharFilterFactory[] charFilters, TokenizerFactory tokenizer, TokenFilterFactory[] filters) {
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
	protected Reader initReaderForNormalization(String fieldName, Reader reader) {
		// same as Lucene8's CustomAnalyzer
		for ( CharFilterFactory charFilter : charFilters ) {
			reader = charFilter.normalize( reader );
		}
		return reader;
	}

	@Override
	protected TokenStream normalize(String fieldName, TokenStream in) {
		// same as Lucene8's CustomAnalyzer
		TokenStream result = in;
		for ( TokenFilterFactory filter : filters ) {
			result = filter.normalize( result );
		}
		return result;
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
