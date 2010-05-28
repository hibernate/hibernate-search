package org.hibernate.search.query.dsl.v2.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.v2.PhraseMatchingContext;
import org.hibernate.search.query.dsl.v2.PhraseTermination;
import org.hibernate.search.query.dsl.v2.RangeMatchingContext;
import org.hibernate.search.query.dsl.v2.RangeTerminationExcludable;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedPhraseMatchingContext implements PhraseMatchingContext {
	private final SearchFactory factory;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;
	private final PhraseQueryContext queryContext;
	private final List<FieldContext> fieldContexts;
	//when a varargs of fields are passed, apply the same customization for all.
	//keep the index of the first context in this queue
	private int firstOfContext = 0;

	public ConnectedPhraseMatchingContext(String fieldName,
											PhraseQueryContext queryContext,
											QueryCustomizer queryCustomizer,
											Analyzer queryAnalyzer,
											SearchFactory factory) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = queryCustomizer;
		this.queryContext = queryContext;
		this.fieldContexts = new ArrayList<FieldContext>(4);
		this.fieldContexts.add( new FieldContext( fieldName ) );
	}

	public PhraseMatchingContext andField(String field) {
		this.fieldContexts.add( new FieldContext( field ) );
		this.firstOfContext = fieldContexts.size() - 1;
		return this;
	}

	public PhraseTermination sentence(String sentence) {
		queryContext.setSentence(sentence);
		return new ConnectedMultiFieldsPhraseQueryBuilder(queryContext, queryAnalyzer, queryCustomizer, fieldContexts);
	}

	public PhraseMatchingContext boostedTo(float boost) {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.getFieldCustomizer().boostedTo( boost );
		}
		return this;
	}

	private List<FieldContext> getCurrentFieldContexts() {
		return fieldContexts.subList( firstOfContext, fieldContexts.size() );
	}

	public PhraseMatchingContext ignoreAnalyzer() {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.setIgnoreAnalyzer( true );
		}
		return this;
	}
}