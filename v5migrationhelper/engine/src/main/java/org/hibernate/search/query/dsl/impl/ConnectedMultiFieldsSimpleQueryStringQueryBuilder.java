/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.backend.lucene.search.spi.LuceneMigrationUtils;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateOptionsStep;
import org.hibernate.search.query.dsl.SimpleQueryStringTermination;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

/**
 * @author Guillaume Smet
 */
public class ConnectedMultiFieldsSimpleQueryStringQueryBuilder implements SimpleQueryStringTermination {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final FieldsContext fieldsContext;

	private final String simpleQueryString;
	private final boolean withAndAsDefaultOperator;

	public ConnectedMultiFieldsSimpleQueryStringQueryBuilder(QueryBuildingContext queryContext,
			QueryCustomizer queryCustomizer, FieldsContext fieldsContext, String simpleQueryString,
			boolean withAndAsDefaultOperator) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldsContext = fieldsContext;

		this.simpleQueryString = simpleQueryString;
		this.withAndAsDefaultOperator = withAndAsDefaultOperator;
	}

	@Override
	public Query createQuery() {
		return LuceneMigrationUtils.toLuceneQuery( createPredicate() );
	}

	private SearchPredicate createPredicate() {
		SearchPredicateFactory factory = queryContext.getScope().predicate();

		SimpleQueryStringPredicateFieldStep<?> fieldStep = factory.simpleQueryString();
		SimpleQueryStringPredicateFieldMoreStep<?, ?> fieldMoreStep = null;
		for ( FieldContext fieldContext : fieldsContext ) {
			fieldMoreStep = fieldContext.applyBoost( fieldStep.field( fieldContext.getField() ) );
		}
		SimpleQueryStringPredicateOptionsStep<?> optionsStep = fieldMoreStep.matching( simpleQueryString )
				.defaultOperator( withAndAsDefaultOperator ? BooleanOperator.AND : BooleanOperator.OR );

		String overriddenAnalyzer = overriddenAnalyzer();
		if ( overriddenAnalyzer != null ) {
			optionsStep = optionsStep.analyzer( overriddenAnalyzer );
		}

		queryCustomizer.applyScoreOptions( optionsStep );
		SearchPredicate predicate = optionsStep.toPredicate();
		return queryCustomizer.applyFilter( factory, predicate );
	}

	private String overriddenAnalyzer() {
		Set<String> effectiveAnalyzers = new HashSet<>();
		String overriddenAnalyzer = null;

		for ( FieldContext fieldContext : fieldsContext ) {
			String fieldName = fieldContext.getField();
			String fieldEffectiveAnalyzer;
			String fieldOverriddenAnalyzer = queryContext.getOverriddenAnalyzer( fieldName );
			if ( fieldOverriddenAnalyzer == null ) {
				fieldEffectiveAnalyzer = queryContext.getOriginalAnalyzer( fieldName );
			}
			else {
				fieldEffectiveAnalyzer = fieldOverriddenAnalyzer;
				if ( overriddenAnalyzer == null ) {
					overriddenAnalyzer = fieldOverriddenAnalyzer;
				}
			}
			if ( fieldEffectiveAnalyzer != null ) {
				effectiveAnalyzers.add( fieldEffectiveAnalyzer );
			}
		}
		if ( overriddenAnalyzer != null && effectiveAnalyzers.size() > 1 ) {
			throw log.unableToOverrideQueryAnalyzerWithMoreThanOneAnalyzerForSimpleQueryStringQueries( effectiveAnalyzers );
		}
		return overriddenAnalyzer;
	}

}
