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

import java.io.Reader;
import java.util.Map;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeSource.AttributeFactory;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public abstract class TestTokenizer extends TokenizerFactory {

	protected TestTokenizer(Map<String, String> args) {
		super( args );
		assureMatchVersion();
		if ( !args.isEmpty() ) {
			throw new IllegalArgumentException( "Unknown parameters: " + args );
		}
	}

	@Override
	public abstract StreamWrappingTokenizer create(AttributeFactory factory, Reader input);

	public static class TestTokenizer1 extends TestTokenizer {
		public TestTokenizer1(Map<String, String> args) {
			super( args );
		}
		@Override
		public StreamWrappingTokenizer create(AttributeFactory factory, Reader input) {
			return new StreamWrappingTokenizer( input, new String[]{ "dog" } );
		}
	}

	public static class TestTokenizer2 extends TestTokenizer {
		public TestTokenizer2(Map<String, String> args) {
			super( args );
		}
		@Override
		public StreamWrappingTokenizer create(AttributeFactory factory, Reader input) {
			return new StreamWrappingTokenizer( input, new String[]{ "cat" } );
		}
	}

	public static class TestTokenizer3 extends TestTokenizer {
		public TestTokenizer3(Map<String, String> args) {
			super( args );
		}
		@Override
		public StreamWrappingTokenizer create(AttributeFactory factory, Reader input) {
			return new StreamWrappingTokenizer( input, new String[]{ "mouse" } );
		}
	}
}
