/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer;

import java.io.Reader;
import java.util.Map;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

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
