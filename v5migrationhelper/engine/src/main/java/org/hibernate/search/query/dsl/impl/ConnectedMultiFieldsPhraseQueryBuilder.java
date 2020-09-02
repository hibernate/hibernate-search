/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.PhrasePredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.query.dsl.PhraseTermination;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMultiFieldsPhraseQueryBuilder
		extends AbstractConnectedMultiFieldsQueryBuilder<PhraseTermination, PhrasePredicateOptionsStep<?>>
		implements PhraseTermination {
	private final PhraseQueryContext phraseContext;

	public ConnectedMultiFieldsPhraseQueryBuilder(QueryBuildingContext queryContext, QueryCustomizer queryCustomizer,
			FieldsContext fieldsContext, PhraseQueryContext phraseContext) {
		super( queryContext, queryCustomizer, fieldsContext );
		this.phraseContext = phraseContext;
	}

	@Override
	protected PhrasePredicateOptionsStep<?> createPredicate(SearchPredicateFactory factory, FieldContext fieldContext) {
		PhrasePredicateOptionsStep<?> optionsStep =
				fieldContext.applyBoost( factory.phrase().field( fieldContext.getField() ) )
						.matching( phraseContext.getSentence() );

		optionsStep = optionsStep.slop( phraseContext.getSlop() );

		if ( fieldContext.skipAnalysis() ) {
			optionsStep = optionsStep.skipAnalysis();
		}
		else {
			String overriddenAnalyzer = queryContext.getOverriddenAnalyzer( fieldContext.getField() );
			if ( overriddenAnalyzer != null ) {
				optionsStep = optionsStep.analyzer( overriddenAnalyzer );
			}
		}

		return optionsStep;
	}

}
