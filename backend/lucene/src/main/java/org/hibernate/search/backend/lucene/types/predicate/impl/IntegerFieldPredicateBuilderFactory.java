/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.types.converter.impl.StandardFieldConverter;

public final class IntegerFieldPredicateBuilderFactory
		extends AbstractStandardLuceneFieldPredicateBuilderFactory<StandardFieldConverter<Integer>> {

	public IntegerFieldPredicateBuilderFactory(StandardFieldConverter<Integer> converter) {
		super( converter );
	}

	@Override
	public IntegerMatchPredicateBuilder createMatchPredicateBuilder(String absoluteFieldPath) {
		return new IntegerMatchPredicateBuilder( absoluteFieldPath, converter );
	}

	@Override
	public IntegerRangePredicateBuilder createRangePredicateBuilder(String absoluteFieldPath) {
		return new IntegerRangePredicateBuilder( absoluteFieldPath, converter );
	}
}
