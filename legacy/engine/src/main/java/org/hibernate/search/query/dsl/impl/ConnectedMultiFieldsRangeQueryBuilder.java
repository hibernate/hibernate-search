/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.dsl.RangeTerminationExcludable;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class ConnectedMultiFieldsRangeQueryBuilder implements RangeTerminationExcludable {
	private final RangeQueryContext rangeContext;
	private final QueryCustomizer queryCustomizer;
	private final List<FieldContext> fieldContexts;
	private final QueryBuildingContext queryContext;

	public ConnectedMultiFieldsRangeQueryBuilder(RangeQueryContext rangeContext,
			QueryCustomizer queryCustomizer,
			List<FieldContext> fieldContexts,
			QueryBuildingContext queryContext) {
		this.rangeContext = rangeContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldContexts = fieldContexts;
		this.queryContext = queryContext;
	}

	@Override
	public RangeTerminationExcludable excludeLimit() {
		if ( rangeContext.getFrom() != null && rangeContext.getTo() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else if ( rangeContext.getFrom() != null ) {
			rangeContext.setExcludeFrom( true );
		}
		else if ( rangeContext.getTo() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else {
			throw new AssertionFailure( "Both from and to clause of a range query are null" );
		}
		return this;
	}

	@Override
	public Query createQuery() {
		final int size = fieldContexts.size();
		final ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
		if ( size == 1 ) {
			return queryCustomizer.setWrappedQuery( createQuery( fieldContexts.get( 0 ), conversionContext ) ).createQuery();
		}
		else {
			BooleanQuery.Builder aggregatedFieldsQueryBuilder = new BooleanQuery.Builder();
			for ( FieldContext fieldContext : fieldContexts ) {
				aggregatedFieldsQueryBuilder.add( createQuery( fieldContext, conversionContext ), BooleanClause.Occur.SHOULD );
			}
			return queryCustomizer.setWrappedQuery( aggregatedFieldsQueryBuilder.build() ).createQuery();
		}
	}

	private Query createQuery(FieldContext fieldContext, ConversionContext conversionContext) {
		final Query perFieldQuery;
		final String fieldName = fieldContext.getField();

		final DocumentBuilderIndexedEntity documentBuilder = queryContext.getDocumentBuilder();
		if ( Helper.requiresNumericQuery( documentBuilder, fieldContext, rangeContext.getFrom(), rangeContext.getTo() ) ) {
			perFieldQuery = createNumericRangeQuery( fieldName, rangeContext );
		}
		else {
			perFieldQuery = createKeywordRangeQuery( fieldName, rangeContext, queryContext, conversionContext, fieldContext );
		}

		return fieldContext.getFieldCustomizer().setWrappedQuery( perFieldQuery ).createQuery();
	}

	private static Query createKeywordRangeQuery(String fieldName, RangeQueryContext rangeContext, QueryBuildingContext queryContext, ConversionContext conversionContext, FieldContext fieldContext) {
		final AnalyzerReference analyzerReference = queryContext.getQueryAnalyzerReference();

		final DocumentBuilderIndexedEntity documentBuilder = queryContext.getDocumentBuilder();

		final String fromString = rangeContext.hasFrom() ?
				fieldContext.objectToString( documentBuilder, rangeContext.getFrom(), conversionContext ) :
				null;
		final String toString = rangeContext.hasTo() ?
				fieldContext.objectToString( documentBuilder, rangeContext.getTo(), conversionContext ) :
				null;

		String lowerTerm;
		String upperTerm;

		if ( analyzerReference.is( LuceneAnalyzerReference.class ) ) {
			final Analyzer queryAnalyzer = analyzerReference.unwrap( LuceneAnalyzerReference.class ).getAnalyzer();

			lowerTerm = fromString == null ?
					null :
					Helper.getAnalyzedTerm( fieldName, fromString, "from", queryAnalyzer, fieldContext );

			upperTerm = toString == null ?
					null :
					Helper.getAnalyzedTerm( fieldName, toString, "to", queryAnalyzer, fieldContext );
		}
		else {
			lowerTerm = fromString == null ? null : fromString;
			upperTerm = toString == null ? null : toString;
		}

		return TermRangeQuery.newStringRange( fieldName, lowerTerm, upperTerm, !rangeContext.isExcludeFrom(), !rangeContext.isExcludeTo() );
	}

	private static Query createNumericRangeQuery(String fieldName, RangeQueryContext rangeContext) {
		return NumericFieldUtils.createNumericRangeQuery(
				fieldName,
				rangeContext.getFrom(),
				rangeContext.getTo(),
				!rangeContext.isExcludeFrom(),
				!rangeContext.isExcludeTo()
		);
	}
}
