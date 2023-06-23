/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Helper class to hide boilerplate code when using Lucene Analyzers.
 *
 * <p>Taken and modified from <i>Lucene in Action</i>.
 *
 * @author Hardy Ferentschik
 * @deprecated Will be removed without replacement.
 */
@Deprecated
public final class AnalyzerUtils {

	private AnalyzerUtils() {
		//not allowed
	}

	public static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	public static List<String> tokenizedTermValues(Analyzer analyzer, String field, String text) throws IOException {
		final List<String> tokenList = new ArrayList<String>();
		final TokenStream stream = analyzer.tokenStream( field, new StringReader( text ) );
		try {
			CharTermAttribute term = stream.addAttribute( CharTermAttribute.class );
			stream.reset();
			while ( stream.incrementToken() ) {
				String s = new String( term.buffer(), 0, term.length() );
				tokenList.add( s );
			}
			stream.end();
		}
		finally {
			stream.close();
		}
		return tokenList;
	}
}
