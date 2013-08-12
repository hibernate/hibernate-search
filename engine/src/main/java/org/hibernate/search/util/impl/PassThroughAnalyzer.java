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
package org.hibernate.search.util.impl;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.Version;

/**
 * Analyzer that applies no operation whatsoever to the flux
 * This is useful for queries operating on non tokenized fields.
 * <p/>
 * TODO there is probably a way to make that much more efficient by
 * reimplementing TokenStream to take the Reader and pass through the flux as a single token
 *
 * @author Emmanuel Bernard
 */
public final class PassThroughAnalyzer extends Analyzer {

	private final Version luceneVersion;

	/**
	 * Create a new PassThroughAnalyzer.
	 *
	 * @param luceneVersion
	 */
	public PassThroughAnalyzer(Version luceneVersion) {
		this.luceneVersion = luceneVersion;
	}

	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		if ( luceneVersion.onOrAfter( Version.LUCENE_31 ) ) {
			return new PassThroughTokenizer( luceneVersion, reader );
		}
		else {
			return new Pre31PassThroughTokenizer( luceneVersion, reader );
		}
	}

	/**
	 * To be used when Lucene's Version >= 3.1
	 * @since 4.2
	 */
	private static class PassThroughTokenizer extends CharTokenizer {
		public PassThroughTokenizer(Version luceneVersion, Reader input) {
			super( luceneVersion, input );
		}
		@Override
		protected boolean isTokenChar(int c) {
			return true;
		}
	}

	/**
	 * To be used when Lucene's Version < 3.1
	 * @since 4.2
	 */
	private static class Pre31PassThroughTokenizer extends CharTokenizer {
		public Pre31PassThroughTokenizer(Version luceneVersion, Reader input) {
			super( luceneVersion, input );
		}

		//@Override not really: will be removed in Lucene 4.0
		@Override
		protected boolean isTokenChar(char c) {
			return true;
		}
	}
}
