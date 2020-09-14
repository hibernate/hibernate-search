/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.query.dsl.TermTermination;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMultiFieldsMatchQueryBuilder
		extends AbstractConnectedMultiFieldsQueryBuilder<TermTermination, MatchPredicateOptionsStep<?>>
		implements TermTermination {

	private final Object value;
	private final TermQueryContext termContext;

	public ConnectedMultiFieldsMatchQueryBuilder(QueryBuildingContext queryContext, QueryCustomizer queryCustomizer,
			FieldsContext fieldsContext, Object value, TermQueryContext termContext) {
		super( queryContext, queryCustomizer, fieldsContext );
		this.value = value;
		this.termContext = termContext;
	}

	@Override
	protected MatchPredicateOptionsStep<?> createPredicate(SearchPredicateFactory factory, FieldContext fieldContext) {
		MatchPredicateOptionsStep<?> optionsStep =
				fieldContext.applyBoost( factory.match().field( fieldContext.getField() ) )
						.matching( value, fieldContext.getValueConvert() );

		if ( TermQueryContext.Approximation.FUZZY.equals( termContext.getApproximation() ) ) {
			optionsStep.fuzzy( termContext.getMaxEditDistance(), termContext.getPrefixLength() );
		}

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
