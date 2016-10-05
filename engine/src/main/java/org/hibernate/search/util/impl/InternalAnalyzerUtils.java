/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.AnalyzerUtils;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Analysis helpers that have no reason to be exposed publicly as {@link AnalyzerUtils} is.
 *
 * @author Yoann Rodiere
 */
public final class InternalAnalyzerUtils {

	private static final Log log = LoggerFactory.make();

	private InternalAnalyzerUtils() {
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
	public static String analyzeSortableValue(Analyzer analyzer, String fieldName, String text) {
		final TokenStream stream = analyzer.tokenStream( fieldName, new StringReader( text ) );
		try {
			try {
				String firstToken = null;
				CharTermAttribute term = stream.addAttribute( CharTermAttribute.class );
				stream.reset();
				if ( stream.incrementToken() ) {
					firstToken = new String( term.buffer(), 0, term.length() );
					if ( stream.incrementToken() ) {
						log.multipleTermsInAnalyzedSortableField( fieldName );
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
			throw log.couldNotAnalyzeSortableField( fieldName, e );
		}
	}

}
