package org.hibernate.search.query.dsl.impl;

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
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * @author Emmanuel Bernard
 */
class Helper {
	/**
	 * return the analyzed value for a given field. If several terms are created, an exception is raised.
	 */
	static String getAnalyzedTerm(String fieldName, String value, String name, Analyzer queryAnalyzer, FieldContext fieldContext) {
		if ( fieldContext.isIgnoreAnalyzer() ) return value;
		
		try {
			final List<String> termsFromText = getAllTermsFromText(
					fieldName, value.toString(), queryAnalyzer
			);
			if (termsFromText.size() > 1) {
				StringBuilder error = new StringBuilder( "The ")
						.append( name )
						.append( " parameter leads to several terms when analyzed: " );
				for ( String term : termsFromText ) {
					error.append( term ).append( ", " );
				}
				final int length = error.length();
				throw new SearchException( error.delete( length - 1, length ).toString() );
			}
			return termsFromText.size() == 0 ? null : termsFromText.get( 0 );
		}
		catch ( IOException e ) {
			throw new AssertionFailure("IO exception while reading String stream??", e);
		}
	}

	static List<String> getAllTermsFromText(String fieldName, String localText, Analyzer analyzer) throws IOException {
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

	static DocumentBuilderIndexedEntity<?> getDocumentBuilder(QueryBuildingContext queryContext) {
		final SearchFactoryImplementor factory = queryContext.getFactory();
		final Class<?> type = queryContext.getEntityType();
		DocumentBuilderIndexedEntity<?> builder = factory.getDocumentBuilderIndexedEntity( type );
		if ( builder == null ) {
			throw new AssertionFailure( "Class in not indexed: " + type );
		}
		return builder;
	}
}
