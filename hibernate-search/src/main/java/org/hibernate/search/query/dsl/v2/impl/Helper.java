package org.hibernate.search.query.dsl.v2.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;

/**
 * @author Emmanuel Bernard
 */
class Helper {
	/**
	 * return the analyzed value for a given field. If several terms are created, an exception is raised.
	 */
	static String getAnalyzedTerm(String fieldName, Object value, String name, Analyzer queryAnalyzer) {
		try {
			final List<String> termsFromText = getAllTermsFromText(
					fieldName, value.toString(), queryAnalyzer
			);
			if (termsFromText.size() > 1) {
				throw new SearchException( "The " + name + " parameter leads to several terms when analyzed");
			}
			return termsFromText.size() == 0 ? null : termsFromText.get( 0 );
		}
		catch ( IOException e ) {
			throw new AssertionFailure("IO exception while reading String stream??", e);
		}
	}

	static List<String> getAllTermsFromText(String fieldName, String localText, Analyzer analyzer) throws IOException {
		//it's better not to apply the analyzer with wildcard as * and ? can be mistakenly removed
		List<String> terms = new ArrayList<String>();

		Reader reader = new StringReader(localText);
		TokenStream stream = analyzer.reusableTokenStream( fieldName, reader);
		TermAttribute attribute = (TermAttribute) stream.addAttribute( TermAttribute.class );
		stream.reset();

		while ( stream.incrementToken() ) {
			if ( attribute.termLength() > 0 ) {
				String term = attribute.term();
				terms.add( term );
			}
		}
		stream.end();
		stream.close();
		return terms;
	}
}
