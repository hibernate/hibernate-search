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
import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.impl.NullEncodingTwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.spi.IgnoreAnalyzerBridge;
import org.hibernate.search.bridge.util.impl.BridgeAdaptorUtils;
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
			BooleanQuery.Builder aggregatedFieldsQueryBuilder = new BooleanQuery.Builder();
			for ( FieldContext fieldContext : fieldsContext ) {
				aggregatedFieldsQueryBuilder.add(
					createQuery( fieldContext, conversionContext ),
					BooleanClause.Occur.SHOULD
				);
			}
			BooleanQuery aggregatedFieldsQuery = aggregatedFieldsQueryBuilder.build();
			return queryCustomizer.setWrappedQuery( aggregatedFieldsQuery ).createQuery();
		}
	}

	private Query createQuery(FieldContext fieldContext, ConversionContext conversionContext) {
		final Query perFieldQuery;
		final DocumentBuilderIndexedEntity documentBuilder = queryContext.getDocumentBuilder();
		final boolean applyTokenization;

		FieldBridge fieldBridge = fieldContext.getFieldBridge() != null ? fieldContext.getFieldBridge() : documentBuilder.getBridge( fieldContext.getField() );
		// Handle non-null numeric values
		if ( value != null ) {
			applyTokenization = fieldContext.applyAnalyzer();
			if ( Helper.requiresNumericQuery( documentBuilder, fieldContext, value ) ) {
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

		if ( !applyTokenization || BridgeAdaptorUtils.unwrapAdaptorOnly( fieldBridge, IgnoreAnalyzerBridge.class ) != null ) {
			perFieldQuery = createTermQuery( fieldContext, searchTerm );
		}
		else {
			// we need to build differentiated queries depending of if the search terms should be analyzed
			// locally or not
			if ( queryContext.getQueryAnalyzerReference().is( RemoteAnalyzerReference.class ) ) {
				perFieldQuery = createRemoteQuery( fieldContext, searchTerm );
			}
			else {
				perFieldQuery = createLuceneQuery( fieldContext, searchTerm );
			}
		}
		return fieldContext.getFieldCustomizer().setWrappedQuery( perFieldQuery ).createQuery();
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
				int maxEditDistance = getMaxEditDistance( term );
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

	private int getMaxEditDistance(String term) {
		int maxEditDistance;
		if ( termContext.getThreshold() != null ) {
			//support legacy withThreshold setting
			maxEditDistance = FuzzyQuery.floatToEdits( termContext.getThreshold(), term.length() );
		}
		else {
			maxEditDistance = termContext.getMaxEditDistance();
		}
		return maxEditDistance;
	}

	private Query createLuceneQuery(FieldContext fieldContext, String searchTerm) {
		Query query;
		List<String> terms = getAllTermsFromText(
				fieldContext.getField(),
				searchTerm,
				queryContext.getQueryAnalyzerReference().unwrap( LuceneAnalyzerReference.class ).getAnalyzer()
		);

		if ( terms.size() == 0 ) {
			throw log.queryWithNoTermsAfterAnalysis( fieldContext.getField(), searchTerm );
		}
		else if ( terms.size() == 1 ) {
			query = createTermQuery( fieldContext, terms.get( 0 ) );
		}
		else {
			BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
			for ( String localTerm : terms ) {
				Query termQuery = createTermQuery( fieldContext, localTerm );
				booleanQueryBuilder.add( termQuery, BooleanClause.Occur.SHOULD );
			}
			query = booleanQueryBuilder.build();
		}
		return query;
	}

	private Query createRemoteQuery(FieldContext fieldContext, String searchTerm) {
		// in the case of wildcard, we just return a WildcardQuery
		if ( termContext.getApproximation() == TermQueryContext.Approximation.WILDCARD ) {
			return new WildcardQuery( new Term( fieldContext.getField(), searchTerm ) );
		}

		RemoteMatchQuery.Builder queryBuilder = new RemoteMatchQuery.Builder();
		queryBuilder
				.field( fieldContext.getField() )
				.searchTerms( searchTerm )
				.analyzerReference(
						queryContext.getOriginalAnalyzerReference().unwrap( RemoteAnalyzerReference.class ),
						queryContext.getQueryAnalyzerReference().unwrap( RemoteAnalyzerReference.class )
				);

		if ( termContext.getApproximation() == TermQueryContext.Approximation.FUZZY ) {
			// TODO: remove the threshold method as it's deprecated and not accurate
			// the max edit distance based on the total searchTerm length which is wrong
			// It might be a good time to consider removing the deprecated threshold method
			queryBuilder.maxEditDistance( getMaxEditDistance( searchTerm ) );
		}

		return queryBuilder.build();
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
