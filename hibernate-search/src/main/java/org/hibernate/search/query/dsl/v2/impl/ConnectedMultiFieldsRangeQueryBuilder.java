package org.hibernate.search.query.dsl.v2.impl;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.query.dsl.v2.RangeTerminationExcludable;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMultiFieldsRangeQueryBuilder implements RangeTerminationExcludable {
	private final RangeQueryContext queryContext;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;
	private final List<FieldContext> fieldContexts;

	public ConnectedMultiFieldsRangeQueryBuilder(RangeQueryContext queryContext, Analyzer queryAnalyzer, QueryCustomizer queryCustomizer, List<FieldContext> fieldContexts) {
		this.queryContext = queryContext;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = queryCustomizer;
		this.fieldContexts = fieldContexts;
	}

	public RangeTerminationExcludable exclude() {
		if ( queryContext.getFrom() != null && queryContext.getTo() != null ) {
			queryContext.setExcludeTo( true );
		}
		else if ( queryContext.getFrom() != null ) {
			queryContext.setExcludeTo( true );
		}
		else if ( queryContext.getTo() != null ) {
			queryContext.setExcludeTo( true );
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
		final Object from = queryContext.getFrom();
		final String lowerTerm = from == null ? null : Helper.getAnalyzedTerm( fieldName, from, "from", queryAnalyzer );
		final Object to = queryContext.getTo();
		final String upperTerm = to == null ? null : Helper.getAnalyzedTerm( fieldName, to, "to", queryAnalyzer );
		perFieldQuery = new TermRangeQuery(
				fieldName,
				lowerTerm,
				upperTerm,
				queryContext.isExcludeFrom(),
				queryContext.isExcludeTo()
		);
		return fieldContext.getFieldCustomizer().setWrappedQuery( perFieldQuery ).createQuery();
	}
}
