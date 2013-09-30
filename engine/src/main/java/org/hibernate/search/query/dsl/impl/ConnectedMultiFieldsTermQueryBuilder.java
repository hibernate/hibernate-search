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
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.hibernate.search.exception.AssertionFailure;
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

	@Override
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
		final FieldBridge fieldBridge = fieldContext.getFieldBridge() != null ? fieldContext.getFieldBridge() : documentBuilder.getBridge( fieldContext.getField() );
		if ( fieldBridge instanceof NumericFieldBridge ) {
			return NumericFieldUtils.createExactMatchQuery( fieldContext.getField(), value );
		}

		final String searchTerm = buildSearchTerm( fieldContext, documentBuilder, conversionContext );

		if ( fieldContext.isIgnoreAnalyzer() ) {
			perFieldQuery = createTermQuery( fieldContext, searchTerm );
		}
		else {
			List<String> terms = getAllTermsFromText(
					fieldContext.getField(), searchTerm, queryContext.getQueryAnalyzer()
			);

			if ( terms.size() == 0 ) {
				throw log.queryWithNoTermsAfterAnalysis( fieldContext.getField(), searchTerm );
			}
			else if ( terms.size() == 1 ) {
				perFieldQuery = createTermQuery( fieldContext, terms.get( 0 ) );
			}
			else {
				BooleanQuery booleanQuery = new BooleanQuery();
				for ( String localTerm : terms ) {
					Query termQuery = createTermQuery( fieldContext, localTerm );
					booleanQuery.add( termQuery, BooleanClause.Occur.SHOULD );
				}
				perFieldQuery = booleanQuery;
			}
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
			return fieldContext.objectToString( documentBuilder, value, conversionContext );
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

	private List<String> getAllTermsFromText(String fieldName, String localText, Analyzer analyzer) {
		//it's better not to apply the analyzer with wildcard as * and ? can be mistakenly removed
		List<String> terms = new ArrayList<String>();
		if ( termContext.getApproximation() == TermQueryContext.Approximation.WILDCARD ) {
			terms.add( localText );
		}
		else {
			try {
				terms = Helper.getAllTermsFromText( fieldName, localText, analyzer );
			}
			catch (IOException e) {
				throw new AssertionFailure( "IO exception while reading String stream??", e );
			}
		}
		return terms;
	}
}
