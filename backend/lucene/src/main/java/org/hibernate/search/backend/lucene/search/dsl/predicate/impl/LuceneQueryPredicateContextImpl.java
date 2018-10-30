/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.predicate.impl;

import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractSearchPredicateTerminalContext;

import org.apache.lucene.search.Query;

final class LuceneQueryPredicateContextImpl
		extends AbstractSearchPredicateTerminalContext<LuceneSearchPredicateBuilder>
		implements SearchPredicateTerminalContext {
	private final LuceneSearchPredicateBuilder builder;

	LuceneQueryPredicateContextImpl(LuceneSearchPredicateFactory factory, Query luceneQuery) {
		super( factory );
		this.builder = factory.fromLuceneQuery( luceneQuery );
	}

	@Override
	protected LuceneSearchPredicateBuilder toImplementation() {
		return builder;
	}
}
