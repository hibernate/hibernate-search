/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.query.dsl.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;

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
		if ( fieldContext.isIgnoreAnalyzer() ) {
			return value;
		}

		try {
			final List<String> termsFromText = getAllTermsFromText(
					fieldName, value, queryAnalyzer
			);
			if ( termsFromText.size() > 1 ) {
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
		catch (IOException e) {
			throw new AssertionFailure("IO exception while reading String stream??", e);
		}
	}

	static List<String> getAllTermsFromText(String fieldName, String localText, Analyzer analyzer) throws IOException {
		List<String> terms = new ArrayList<String>();

		// Can't deal with null at this point. Likely returned by some FieldBridge not recognizing the type.
		if ( localText == null ) {
			throw new SearchException( "Search parameter on field " + fieldName + " could not be converted. " +
					"Are the parameter and the field of the same type?" +
					"Alternatively, apply the ignoreFieldBridge() option to " +
					"pass String parameters" );
		}
		Reader reader = new StringReader(localText);
		TokenStream stream = analyzer.reusableTokenStream( fieldName, reader);
		CharTermAttribute attribute = stream.addAttribute( CharTermAttribute.class );
		stream.reset();

		while ( stream.incrementToken() ) {
			if ( attribute.length() > 0 ) {
				String term = new String( attribute.buffer(), 0, attribute.length() );
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
		EntityIndexBinding indexBinding = factory.getIndexBinding( type );
		if ( indexBinding == null ) {
			throw new AssertionFailure( "Class is not indexed: " + type );
		}
		return indexBinding.getDocumentBuilder();
	}
}
