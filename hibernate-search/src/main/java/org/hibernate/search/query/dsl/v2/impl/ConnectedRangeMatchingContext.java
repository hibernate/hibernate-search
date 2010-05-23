package org.hibernate.search.query.dsl.v2.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.v2.RangeMatchingContext;
import org.hibernate.search.query.dsl.v2.RangeTerminationExcludable;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedRangeMatchingContext implements RangeMatchingContext {
	private final SearchFactory factory;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;
	private final RangeQueryContext queryContext;
	private final List<FieldContext> fieldContexts;
	//when a varargs of fields are passed, apply the same customization for all.
	//keep the index of the first context in this queue
	private int firstOfContext = 0;

	public ConnectedRangeMatchingContext(String fieldName,
										 QueryCustomizer queryCustomizer,
										 Analyzer queryAnalyzer,
										 SearchFactory factory) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = queryCustomizer;
		this.queryContext = new RangeQueryContext();
		this.fieldContexts = new ArrayList<FieldContext>(4);
		this.fieldContexts.add( new FieldContext( fieldName ) );
	}

	public RangeMatchingContext andField(String field) {
		this.fieldContexts.add( new FieldContext( field ) );
		this.firstOfContext = fieldContexts.size() - 1;
		return this;
	}

	public <T> FromRangeContext<T> from(T from) {
		queryContext.setFrom( from );
		return new ConnectedFromRangeContext<T>(this);
	}

	static class ConnectedFromRangeContext<T> implements FromRangeContext<T> {
		private ConnectedRangeMatchingContext mother;

		ConnectedFromRangeContext(ConnectedRangeMatchingContext mother) {
			this.mother = mother;
		}

		public RangeTerminationExcludable to(T to) {
			mother.queryContext.setTo(to);
			return new ConnectedMultiFieldsRangeQueryBuilder(
					mother.queryContext,
					mother.queryAnalyzer,
					mother.queryCustomizer,
					mother.fieldContexts);
		}

		public FromRangeContext<T> exclude() {
			mother.queryContext.setExcludeFrom( true );
			return this;
		}
	}

	public RangeTerminationExcludable below(Object below) {
		queryContext.setTo( below );
		return new ConnectedMultiFieldsRangeQueryBuilder(queryContext, queryAnalyzer, queryCustomizer, fieldContexts);
	}

	public RangeTerminationExcludable above(Object above) {
		queryContext.setFrom( above );
		return new ConnectedMultiFieldsRangeQueryBuilder(queryContext, queryAnalyzer, queryCustomizer, fieldContexts);
	}

	public RangeMatchingContext boostedTo(float boost) {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.getFieldCustomizer().boostedTo( boost );
		}
		return this;
	}

	private List<FieldContext> getCurrentFieldContexts() {
		return fieldContexts.subList( firstOfContext, fieldContexts.size() );
	}

	public RangeMatchingContext ignoreAnalyzer() {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.setIgnoreAnalyzer( true );
		}
		return this;
	}
}
