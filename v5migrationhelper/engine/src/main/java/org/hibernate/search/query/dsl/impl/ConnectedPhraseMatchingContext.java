/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.PhraseMatchingContext;
import org.hibernate.search.query.dsl.PhraseTermination;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedPhraseMatchingContext implements PhraseMatchingContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final PhraseQueryContext phraseContext;
	private final FieldsContext fieldsContext;

	public ConnectedPhraseMatchingContext(String fieldName,
			PhraseQueryContext phraseContext,
			QueryCustomizer queryCustomizer,
			QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.phraseContext = phraseContext;
		this.fieldsContext = new FieldsContext( new String[] { fieldName }, queryContext );
	}

	@Override
	public PhraseMatchingContext andField(String field) {
		this.fieldsContext.add( field );
		return this;
	}

	@Override
	public PhraseTermination sentence(String sentence) {
		phraseContext.setSentence( sentence );
		return new ConnectedMultiFieldsPhraseQueryBuilder( queryContext, queryCustomizer, fieldsContext, phraseContext );
	}

	@Override
	public PhraseMatchingContext boostedTo(float boost) {
		fieldsContext.boostedTo( boost );
		return this;
	}

	@Override
	public PhraseMatchingContext ignoreAnalyzer() {
		fieldsContext.ignoreAnalyzer();
		return this;
	}

	@Override
	public PhraseMatchingContext ignoreFieldBridge() {
		fieldsContext.ignoreFieldBridge();
		return this;
	}
}
