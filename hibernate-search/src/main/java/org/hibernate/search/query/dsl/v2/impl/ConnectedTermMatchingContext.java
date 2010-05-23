package org.hibernate.search.query.dsl.v2.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.v2.TermMatchingContext;
import org.hibernate.search.query.dsl.v2.TermTermination;

/**
* @author Emmanuel Bernard
*/
public class ConnectedTermMatchingContext implements TermMatchingContext {
	private final SearchFactory factory;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext queryContext;
	private final List<FieldContext> fieldContexts;
	//when a varargs of fields are passed, apply the same customization for all.
	//keep the index of the first context in this queue
	private int firstOfContext = 0;

	public ConnectedTermMatchingContext(TermQueryContext queryContext,
			String field, QueryCustomizer queryCustomizer, Analyzer queryAnalyzer, SearchFactory factory) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = queryCustomizer;
		this.queryContext = queryContext;
		this.fieldContexts = new ArrayList<FieldContext>(4);
		this.fieldContexts.add( new FieldContext( field ) );
	}

	public ConnectedTermMatchingContext(TermQueryContext queryContext,
			String[] fields, QueryCustomizer queryCustomizer, Analyzer queryAnalyzer, SearchFactory factory) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = queryCustomizer;
		this.queryContext = queryContext;
		this.fieldContexts = new ArrayList<FieldContext>(fields.length);
		for (String field : fields) {
			this.fieldContexts.add( new FieldContext( field ) );
		}
	}

	public TermTermination matching(String text) {
		return new ConnectedMultiFieldsTermQueryBuilder( queryContext, text, fieldContexts, queryCustomizer, queryAnalyzer, factory);
	}

	public TermMatchingContext andField(String field) {
		this.fieldContexts.add( new FieldContext( field ) );
		this.firstOfContext = fieldContexts.size() - 1;
		return this;
	}

	public TermMatchingContext boostedTo(float boost) {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.getFieldCustomizer().boostedTo( boost );
		}
		return this;
	}

	private List<FieldContext> getCurrentFieldContexts() {
		return fieldContexts.subList( firstOfContext, fieldContexts.size() );
	}

	public TermMatchingContext ignoreAnalyzer() {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.setIgnoreAnalyzer( true );
		}
		return this;
	}
}
