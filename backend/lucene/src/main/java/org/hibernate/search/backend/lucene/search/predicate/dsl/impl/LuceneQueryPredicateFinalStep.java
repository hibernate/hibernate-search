/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.dsl.impl;

import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;

import org.apache.lucene.search.Query;

final class LuceneQueryPredicateFinalStep
		extends AbstractPredicateFinalStep<LuceneSearchPredicateBuilder>
		implements PredicateFinalStep {
	private final LuceneSearchPredicateBuilder builder;

	LuceneQueryPredicateFinalStep(LuceneSearchPredicateBuilderFactory factory, Query luceneQuery) {
		super( factory );
		this.builder = factory.fromLuceneQuery( luceneQuery );
	}

	@Override
	protected LuceneSearchPredicateBuilder toImplementation() {
		return builder;
	}
}
