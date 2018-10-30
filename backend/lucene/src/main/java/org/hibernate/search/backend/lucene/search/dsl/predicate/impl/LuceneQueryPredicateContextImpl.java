/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.predicate.impl;

import java.util.function.Supplier;

import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractObjectCreatingSearchPredicateContributor;

import org.apache.lucene.search.Query;

final class LuceneQueryPredicateContextImpl<N>
		extends AbstractObjectCreatingSearchPredicateContributor<LuceneSearchPredicateBuilder>
		implements SearchPredicateTerminalContext<N> {
	private final Supplier<N> nextContextSupplier;
	private final LuceneSearchPredicateBuilder builder;

	LuceneQueryPredicateContextImpl(LuceneSearchPredicateFactory factory, Supplier<N> nextContextSupplier,
			Query luceneQuery) {
		super( factory );
		this.nextContextSupplier = nextContextSupplier;
		this.builder = factory.fromLuceneQuery( luceneQuery );
	}

	@Override
	public final N end() {
		return nextContextSupplier.get();
	}

	@Override
	protected LuceneSearchPredicateBuilder doContribute() {
		return builder;
	}
}
