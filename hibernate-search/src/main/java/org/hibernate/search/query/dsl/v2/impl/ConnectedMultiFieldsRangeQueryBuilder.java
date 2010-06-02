package org.hibernate.search.query.dsl.v2.impl;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.query.dsl.v2.RangeTerminationExcludable;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMultiFieldsRangeQueryBuilder implements RangeTerminationExcludable {
	private final RangeQueryContext rangeContext;
	private final QueryCustomizer queryCustomizer;
	private final List<FieldContext> fieldContexts;
	private final QueryBuildingContext queryContext;

	public ConnectedMultiFieldsRangeQueryBuilder(RangeQueryContext rangeContext,
												 QueryCustomizer queryCustomizer, List<FieldContext> fieldContexts,
												 QueryBuildingContext queryContext) {
		this.rangeContext = rangeContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldContexts = fieldContexts;
		this.queryContext = queryContext;
	}

	public RangeTerminationExcludable exclude() {
		if ( rangeContext.getFrom() != null && rangeContext.getTo() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else if ( rangeContext.getFrom() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else if ( rangeContext.getTo() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else {
			throw new AssertionFailure( "Both from and to clause of a range query are null" );
		}
		return this;
	}

	public Query createQuery() {
		final int size = fieldContexts.size();
		if ( size == 1 ) {
			return queryCustomizer.setWrappedQuery( createQuery( fieldContexts.get( 0 ) ) ).createQuery();
		}
		else {
			BooleanQuery aggregatedFieldsQuery = new BooleanQuery( );
			for ( FieldContext fieldContext : fieldContexts ) {
				aggregatedFieldsQuery.add( createQuery( fieldContext ), BooleanClause.Occur.SHOULD );
			}
			return  queryCustomizer.setWrappedQuery( aggregatedFieldsQuery ).createQuery();
		}
	}

	public Query createQuery(FieldContext fieldContext) {
		final Query perFieldQuery;
		final String fieldName = fieldContext.getField();
		final Analyzer queryAnalyzer = queryContext.getQueryAnalyzer();
		final Object from = rangeContext.getFrom();
		final String lowerTerm = from == null ? null : Helper.getAnalyzedTerm( fieldName, from, "from", queryAnalyzer );
		final Object to = rangeContext.getTo();
		final String upperTerm = to == null ? null : Helper.getAnalyzedTerm( fieldName, to, "to", queryAnalyzer );
		perFieldQuery = new TermRangeQuery(
				fieldName,
				lowerTerm,
				upperTerm,
				!rangeContext.isExcludeFrom(),
				!rangeContext.isExcludeTo()
		);
		return fieldContext.getFieldCustomizer().setWrappedQuery( perFieldQuery ).createQuery();
	}
}
