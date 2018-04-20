/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.util.impl;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * Analysis helpers that have no reason to be exposed publicly.
 *
 * @author Yoann Rodiere
 */
public final class AnalyzerUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private AnalyzerUtils() {
		// Not used
	}

	/**
	 * Returns the first token resulting from the analysis, logging a warning if there are more than one token.
	 *
	 * @param analyzer the Lucene analyzer to use
	 * @param fieldName the name of the field: might affect the analyzer behavior
	 * @param text the value to analyze
	 * @return the first token resulting from the analysis
	 *
	 * @throws SearchException if a problem occurs when analyzing the sortable field's value.
	 */
	public static String normalize(Analyzer analyzer, String fieldName, String text) {
		final TokenStream stream = analyzer.tokenStream( fieldName, new StringReader( text ) );
		try {
			try {
				String firstToken = null;
				CharTermAttribute term = stream.addAttribute( CharTermAttribute.class );
				stream.reset();
				if ( stream.incrementToken() ) {
					firstToken = new String( term.buffer(), 0, term.length() );
					if ( stream.incrementToken() ) {
						log.multipleTermsDetectedDuringNormalization( fieldName );
					}
					else {
						stream.end();
					}
				}
				return firstToken;
			}
			finally {
				stream.close();
			}
		}
		catch (SearchException | IOException e) {
			throw log.couldNotNormalizeField( fieldName, e );
		}
	}
}
