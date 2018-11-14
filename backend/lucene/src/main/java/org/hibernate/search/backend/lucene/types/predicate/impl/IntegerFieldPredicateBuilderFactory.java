/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;

public final class IntegerFieldPredicateBuilderFactory
		extends AbstractStandardLuceneFieldPredicateBuilderFactory<LuceneFieldConverter<?, Integer>> {

	public IntegerFieldPredicateBuilderFactory(LuceneFieldConverter<?, Integer> converter) {
		super( converter );
	}

	@Override
	public IntegerMatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new IntegerMatchPredicateBuilder( searchContext, absoluteFieldPath, converter );
	}

	@Override
	public IntegerRangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new IntegerRangePredicateBuilder( searchContext, absoluteFieldPath, converter );
	}
}
