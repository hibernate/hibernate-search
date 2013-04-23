/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import java.util.Collections;
import java.util.Map;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.solr.analysis.TokenizerFactory;

/**
 * @author Emmanuel Bernard
 */
public abstract class TestTokenizer extends Tokenizer implements TokenizerFactory {
	private final CharTermAttribute termAttribute = addAttribute( CharTermAttribute.class );
	private int i = 0;

	public TestTokenizer(Reader input) {
		super( input );
	}

	public abstract String[] getTokens();

	@Override
	public final boolean incrementToken() throws IOException {
		if ( i < getTokens().length ) {
			clearAttributes();
			termAttribute.append( getTokens()[i] );
			i++;
			return true;
		}
		return false;
	}

	@Override
	public void init(Map<String, String> args) {
	}

	@Override
	public Map<String, String> getArgs() {
		return Collections.emptyMap();
	}

	public static class TestTokenizer1 extends TestTokenizer {
		private final String[] tokens = { "dog" };

		public TestTokenizer1() {
			super( null );
		}

		@Override
		public String[] getTokens() {
			return tokens;
		}

		@Override
		public Tokenizer create(Reader input) {
			return this;
		}
	}

	public static class TestTokenizer2 extends TestTokenizer {
		private final String[] tokens = { "cat" };

		public TestTokenizer2() {
			super( null );
		}

		@Override
		public String[] getTokens() {
			return tokens;
		}

		@Override
		public Tokenizer create(Reader input) {
			return this;
		}
	}

	public static class TestTokenizer3 extends TestTokenizer {
		private final String[] tokens = { "mouse" };

		public TestTokenizer3() {
			super( null );
		}

		@Override
		public String[] getTokens() {
			return tokens;
		}

		@Override
		public Tokenizer create(Reader input) {
			return this;
		}
	}
}
