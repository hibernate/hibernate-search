/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;

/**
 * @author Emmanuel Bernard
 */
final class Helper {

	private Helper() {
		//not allowed
	}

	/**
	 * return the analyzed value for a given field. If several terms are created, an exception is raised.
	 */
	static String getAnalyzedTerm(String fieldName, String value, String name, Analyzer queryAnalyzer, FieldContext fieldContext) {
		if ( !fieldContext.applyAnalyzer() ) {
			return value;
		}

		try {
			final List<String> termsFromText = getAllTermsFromText(
					fieldName, value, queryAnalyzer
			);
			if ( termsFromText.size() > 1 ) {
				StringBuilder error = new StringBuilder( "The " )
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
		catch (IOException e) {
			throw new AssertionFailure( "IO exception while reading String stream??", e );
		}
	}

	static List<String> getAllTermsFromText(String fieldName, String localText, Analyzer analyzer) throws IOException {
		List<String> terms = new ArrayList<String>();

		// Can't deal with null at this point. Likely returned by some FieldBridge not recognizing the type.
		if ( localText == null ) {
			throw new SearchException( "Search parameter on field '" + fieldName + "' could not be converted. " +
					"Are the parameter and the field of the same type?" +
					"Alternatively, apply the ignoreFieldBridge() option to " +
					"pass String parameters" );
		}
		final Reader reader = new StringReader( localText );
		final TokenStream stream = analyzer.tokenStream( fieldName, reader );
		try {
			CharTermAttribute attribute = stream.addAttribute( CharTermAttribute.class );
			stream.reset();
			while ( stream.incrementToken() ) {
				if ( attribute.length() > 0 ) {
					String term = new String( attribute.buffer(), 0, attribute.length() );
					terms.add( term );
				}
			}
			stream.end();
		}
		finally {
			stream.close();
		}
		return terms;
	}

	static boolean requiresNumericQuery(DocumentBuilderIndexedEntity documentBuilder, FieldContext fieldContext, Object ... searchedValues) {
		String fieldName = fieldContext.getField();

		FieldBridge overriddenFieldBridge = fieldContext.getFieldBridge();

		/*
		 * If using the original field bridge, rely on metadata (if any).
		 *
		 * It would be more logical to use metadata in any case, but we actually have
		 * some bridges that do not abide by metadata.
		 * For instance some bridges index string values for numeric fields when
		 * they encounter null values (see ), and we want to allow users to
		 * query those values. Thus ignoreFieldBridge() provides users with
		 * a way to disable metadata inspection...
		 */
		if ( overriddenFieldBridge == null && !fieldContext.isIgnoreFieldBridge() ) {
			DocumentFieldMetadata metadata = documentBuilder.getTypeMetadata().getDocumentFieldMetadataFor( fieldName );
			if ( metadata != null ) {
				return metadata.isNumeric();
			}

			// We may have a field bridge, but no metadata, in particular for null embeddeds
			FieldBridge bridge = documentBuilder.getBridge( fieldName );
			if ( bridge != null ) {
				return NumericFieldUtils.isNumericFieldBridge( bridge );
			}
		}

		// If using an overridden field bridge, guess numeric-ness from this bridge.
		if ( overriddenFieldBridge != null ) {
			return NumericFieldUtils.isNumericFieldBridge( overriddenFieldBridge );
		}

		// If the above wasn't conclusive, guess from the given value
		if ( searchedValues != null ) {
			for ( Object searchedValue : searchedValues ) {
				if ( NumericFieldUtils.requiresNumericRangeQuery( searchedValue ) ) {
					return true;
				}
			}
		}

		return false;
	}

}
