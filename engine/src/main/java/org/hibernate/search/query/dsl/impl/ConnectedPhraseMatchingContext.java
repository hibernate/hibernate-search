/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.query.dsl.PhraseMatchingContext;
import org.hibernate.search.query.dsl.PhraseTermination;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedPhraseMatchingContext implements PhraseMatchingContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final PhraseQueryContext phraseContext;
	private final List<FieldContext> fieldContexts;
	//when a varargs of fields are passed, apply the same customization for all.
	//keep the index of the first context in this queue
	private int firstOfContext = 0;

	public ConnectedPhraseMatchingContext(String fieldName,
											PhraseQueryContext phraseContext,
											QueryCustomizer queryCustomizer,
											QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.phraseContext = phraseContext;
		this.fieldContexts = new ArrayList<FieldContext>(4);
		this.fieldContexts.add( new FieldContext( fieldName, queryContext ) );
	}

	@Override
	public PhraseMatchingContext andField(String field) {
		this.fieldContexts.add( new FieldContext( field, queryContext ) );
		this.firstOfContext = fieldContexts.size() - 1;
		return this;
	}

	@Override
	public PhraseTermination sentence(String sentence) {
		phraseContext.setSentence( sentence );
		return new ConnectedMultiFieldsPhraseQueryBuilder( phraseContext, queryCustomizer, fieldContexts, queryContext );
	}

	@Override
	public PhraseMatchingContext boostedTo(float boost) {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.getFieldCustomizer().boostedTo( boost );
		}
		return this;
	}

	private List<FieldContext> getCurrentFieldContexts() {
		return fieldContexts.subList( firstOfContext, fieldContexts.size() );
	}

	@Override
	public PhraseMatchingContext ignoreAnalyzer() {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.setIgnoreAnalyzer( true );
		}
		return this;
	}

	@Override
	public PhraseMatchingContext ignoreFieldBridge() {
		//this is a no-op
		return this;
	}
}
