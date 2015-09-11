/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.NumericEncodingDateBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.builtin.impl.NullEncodingTwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.time.impl.NumericTimeBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
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
	private final FieldsContext fieldsContext;
	private final QueryBuildingContext queryContext;

	public ConnectedMultiFieldsTermQueryBuilder(TermQueryContext termContext,
												Object value,
												FieldsContext fieldsContext,
												QueryCustomizer queryCustomizer,
												QueryBuildingContext queryContext) {
		this.termContext = termContext;
		this.value = value;
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldsContext = fieldsContext;
	}

	@Override
	public Query createQuery() {
		final int size = fieldsContext.size();
		final ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
		if ( size == 1 ) {
			return queryCustomizer.setWrappedQuery( createQuery( fieldsContext.getFirst(), conversionContext ) ).createQuery();
		}
		else {
			BooleanQuery aggregatedFieldsQuery = new BooleanQuery();
			for ( FieldContext fieldContext : fieldsContext ) {
				aggregatedFieldsQuery.add( createQuery( fieldContext, conversionContext ), BooleanClause.Occur.SHOULD );
			}
			return queryCustomizer.setWrappedQuery( aggregatedFieldsQuery ).createQuery();
		}
	}

	private Query createQuery(FieldContext fieldContext, ConversionContext conversionContext) {
		final Query perFieldQuery;
		final DocumentBuilderIndexedEntity documentBuilder = Helper.getDocumentBuilder( queryContext );
		final boolean applyTokenization;

		FieldBridge fieldBridge = fieldContext.getFieldBridge() != null ? fieldContext.getFieldBridge() : documentBuilder.getBridge( fieldContext.getField() );
		// Handle non-null numeric values
		if ( value != null ) {
			applyTokenization = fieldContext.applyAnalyzer();
			if ( fieldBridge instanceof NullEncodingTwoWayFieldBridge ) {
				fieldBridge = ( (NullEncodingTwoWayFieldBridge) fieldBridge ).unwrap();
			}
			if ( isNumericBridge( fieldBridge ) ) {
				return NumericFieldUtils.createExactMatchQuery( fieldContext.getField(), value );
			}
		}
		else {
			applyTokenization = false;
			if ( fieldBridge instanceof NullEncodingTwoWayFieldBridge ) {
				NullEncodingTwoWayFieldBridge nullEncodingBridge = (NullEncodingTwoWayFieldBridge) fieldBridge;
				validateNullValueIsSearchable( fieldContext );
				return nullEncodingBridge.buildNullQuery( fieldContext.getField() );
			}
		}

		validateNullValueIsSearchable( fieldContext );
		final String searchTerm = buildSearchTerm( fieldContext, documentBuilder, conversionContext );

		if ( !applyTokenization ) {
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

	private static boolean isNumericBridge(FieldBridge fieldBridge) {
		return fieldBridge instanceof NumericFieldBridge
				|| fieldBridge instanceof NumericTimeBridge
				|| fieldBridge instanceof NumericEncodingDateBridge;
	}

	private void validateNullValueIsSearchable(FieldContext fieldContext) {
		if ( fieldContext.isIgnoreFieldBridge() ) {
			if ( value == null ) {
				throw log.unableToSearchOnNullValueWithoutFieldBridge( fieldContext.getField() );
			}
		}
	}

	private String buildSearchTerm(FieldContext fieldContext, DocumentBuilderIndexedEntity documentBuilder, ConversionContext conversionContext) {
		if ( fieldContext.isIgnoreFieldBridge() ) {
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
				int maxEditDistance;
				if ( termContext.getThreshold() != null ) {
					//support legacy withThreshold setting
					maxEditDistance = FuzzyQuery.floatToEdits( termContext.getThreshold(), term.length() );
				}
				else {
					maxEditDistance = termContext.getMaxEditDistance();
				}
				query = new FuzzyQuery(
						new Term( fieldName, term ),
						maxEditDistance,
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
