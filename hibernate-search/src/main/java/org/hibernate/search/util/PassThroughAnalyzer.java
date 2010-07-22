package org.hibernate.search.util;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.AttributeSource;

/**
 * Analyzer that applies no operation whatsoever to the flux
 * This is useful for queries operating on non tokenized fields.
 *
 * TODO there is probably a way to make that much more efficient by
 * reimplementing TokenStream to take the Reader and pass through the flux as a single token
 *
 * @author Emmanuel Bernard
 */
public class PassThroughAnalyzer extends Analyzer {

	

	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		return new PassThroughTokenizer(reader);
	}

	private static class PassThroughTokenizer extends CharTokenizer {
		public PassThroughTokenizer(Reader input) {
			super( input );
		}

		@Override
		protected boolean isTokenChar(char c) {
			return true;
		}
	}
}
