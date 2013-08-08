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
import java.io.StringReader;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.query.dsl.TermTermination;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMultiFieldsTermQueryBuilder implements TermTermination {

	private static final Log log = LoggerFactory.make();

	private final Object value;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext termContext;
	private final List<FieldContext> fieldContexts;
	private final QueryBuildingContext queryContext;

	public ConnectedMultiFieldsTermQueryBuilder(TermQueryContext termContext,
												Object value,
												List<FieldContext> fieldContexts,
												QueryCustomizer queryCustomizer,
												QueryBuildingContext queryContext) {
		this.termContext = termContext;
		this.value = value;
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldContexts = fieldContexts;
	}

	public Query createQuery() {
		final int size = fieldContexts.size();
		final ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
		if ( size == 1 ) {
			return queryCustomizer.setWrappedQuery( createQuery( fieldContexts.get( 0 ), conversionContext ) ).createQuery();
		}
		else {
			BooleanQuery aggregatedFieldsQuery = new BooleanQuery();
			for ( FieldContext fieldContext : fieldContexts ) {
				aggregatedFieldsQuery.add( createQuery( fieldContext, conversionContext ), BooleanClause.Occur.SHOULD );
			}
			return queryCustomizer.setWrappedQuery( aggregatedFieldsQuery ).createQuery();
		}
	}

	private Query createQuery(FieldContext fieldContext, ConversionContext conversionContext) {
		final Query perFieldQuery;
		final DocumentBuilderIndexedEntity<?> documentBuilder = Helper.getDocumentBuilder( queryContext );
		FieldBridge fieldBridge = documentBuilder.getBridge( fieldContext.getField() );
		if ( fieldBridge instanceof NumericFieldBridge ) {
			return NumericFieldUtils.createExactMatchQuery( fieldContext.getField(), value );
		}

		String searchTerm = buildSearchTerm( fieldContext, documentBuilder, conversionContext );

		if ( fieldContext.isIgnoreAnalyzer() || termContext.getApproximation() == TermQueryContext.Approximation.WILDCARD ) {
			perFieldQuery = createTermQuery( fieldContext, searchTerm );
		}
		else {
			perFieldQuery = getFieldQuery( fieldContext, searchTerm, queryContext.getQueryAnalyzer() );
		}
		return fieldContext.getFieldCustomizer().setWrappedQuery( perFieldQuery ).createQuery();
	}

	private String buildSearchTerm(FieldContext fieldContext, DocumentBuilderIndexedEntity<?> documentBuilder, ConversionContext conversionContext) {
		if ( fieldContext.isIgnoreFieldBridge() ) {
			if ( value == null ) {
				throw new SearchException(
						"Unable to search for null token on field "
								+ fieldContext.getField() + " if field bridge is ignored."
				);
			}
			String stringform = value.toString();
			if ( stringform == null ) {
				throw new SearchException(
						"When ignoreFieldBridge() is enabled, toString() on the value is used: the returned string must not be null: " +
						"on field " + fieldContext.getField() );
			}
			return stringform;
		}
		else {
			// need to go via the appropriate bridge, because value is an object, eg boolean, and must be converted to a string first
			return documentBuilder.objectToString( fieldContext.getField(), value, conversionContext );
		}
	}

	/**
	 * This is a simplified version of org.apache.lucene.queryparser.classic.QueryParser#getFieldQuery()
	 * adapted to work in the Hibernate Search context.
	 * 
	 * It is based on the QueryParser of Lucene 4.4.0.
	 */
	protected Query getFieldQuery(FieldContext fieldContext, String queryText, Analyzer analyzer) {
		final String field = fieldContext.getField();

		TokenStream source;
		try {
			source = analyzer.reusableTokenStream( field, new StringReader( queryText ) );
			source.reset();
		}
		catch ( IOException e ) {
			source = analyzer.tokenStream( field, new StringReader( queryText ) );
		}
		CachingTokenFilter buffer = new CachingTokenFilter( source );
		CharTermAttribute termAtt = null;
		PositionIncrementAttribute posIncrAtt = null;
		int numTokens = 0;

		boolean success = false;
		try {
			buffer.reset();
			success = true;
		}
		catch ( IOException e ) {
			// success==false if we hit an exception
		}
		if ( success ) {
			if ( buffer.hasAttribute( CharTermAttribute.class ) ) {
				termAtt = buffer.getAttribute( CharTermAttribute.class );
			}
			if ( buffer.hasAttribute( PositionIncrementAttribute.class ) ) {
				posIncrAtt = buffer.getAttribute( PositionIncrementAttribute.class );
			}
		}

		int positionCount = 0;

		boolean hasMoreTokens = false;
		if ( termAtt != null ) {
			try {
				hasMoreTokens = buffer.incrementToken();
				while ( hasMoreTokens ) {
					numTokens++;
					int positionIncrement = ( posIncrAtt != null ) ? posIncrAtt.getPositionIncrement() : 1;
					if ( positionIncrement != 0 ) {
						positionCount += positionIncrement;
					}
					hasMoreTokens = buffer.incrementToken();
				}
			}
			catch ( IOException e ) {
				// ignore
			}
		}
		try {
			// rewind the buffer stream
			buffer.reset();

			// close original stream - all tokens buffered
			source.close();
		}
		catch ( IOException e ) {
			// ignore
		}

		if ( numTokens == 0 ) {
			throw log.queryWithNoTermsAfterAnalysis( fieldContext.getField(), queryText );
		}
		else if ( numTokens == 1 ) {
			String term = null;
			try {
				boolean hasNext = buffer.incrementToken();
				assert hasNext == true;
				term = termAtt.toString();
			}
			catch ( IOException e ) {
				// safe to ignore, because we know the number of tokens
			}
			return createTermQuery( fieldContext, term );
		}
		else {
			if (positionCount == 1) {
				BooleanQuery q = new BooleanQuery( true );

				BooleanClause.Occur occur = positionCount > 1 && fieldContext.isWithAllTerms() ? BooleanClause.Occur.MUST
						: BooleanClause.Occur.SHOULD;

				for ( int i = 0; i < numTokens; i++ ) {
					String term = null;
					try {
						boolean hasNext = buffer.incrementToken();
						assert hasNext == true;
						term = termAtt.toString();
					}
					catch ( IOException e ) {
						// safe to ignore, because we know the number of tokens
					}

					Query currentQuery = createTermQuery( fieldContext, term );
					q.add( currentQuery, occur );
				}
				return q;
			}
			else {
				// multiple positions
				BooleanQuery q = new BooleanQuery( false );
				final BooleanClause.Occur occur = fieldContext.isWithAllTerms() ? BooleanClause.Occur.MUST
						: BooleanClause.Occur.SHOULD;
				Query currentQuery = null;
				for ( int i = 0; i < numTokens; i++ ) {
					String term = null;
					try {
						boolean hasNext = buffer.incrementToken();
						assert hasNext == true;
						term = termAtt.toString();
					}
					catch ( IOException e ) {
						// safe to ignore, because we know the number of tokens
					}
					if ( posIncrAtt != null && posIncrAtt.getPositionIncrement() == 0 ) {
						if ( !( currentQuery instanceof BooleanQuery ) ) {
							Query t = currentQuery;
							currentQuery = new BooleanQuery( true );
							( (BooleanQuery) currentQuery ).add( t, BooleanClause.Occur.SHOULD );
						}
						( (BooleanQuery) currentQuery ).add(
								createTermQuery( fieldContext, term ),
								BooleanClause.Occur.SHOULD );
					}
					else {
						if ( currentQuery != null ) {
							q.add( currentQuery, occur );
						}
						currentQuery = createTermQuery( fieldContext, term );
					}
				}
				q.add( currentQuery, occur );
				return q;
			}
		}
	}

	private Query createTermQuery(FieldContext fieldContext, String term) {
		Query query;
		final String fieldName = fieldContext.getField();
		switch ( termContext.getApproximation() ) {
			case EXACT:
				query = new TermQuery( new Term( fieldName, term ) );
				break;
			case WILDCARD:
				query = new WildcardQuery( new Term( fieldName, term ) );
				break;
			case FUZZY:
				query = new FuzzyQuery(
						new Term( fieldName, term ),
						termContext.getThreshold(),
						termContext.getPrefixLength()
				);
				break;
			default:
				throw new AssertionFailure( "Unknown approximation: " + termContext.getApproximation() );
		}
		return query;
	}

}
