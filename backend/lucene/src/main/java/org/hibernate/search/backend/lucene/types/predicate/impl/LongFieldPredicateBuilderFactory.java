/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.converter.impl.StandardFieldConverter;

public final class LongFieldPredicateBuilderFactory
		extends AbstractStandardLuceneFieldPredicateBuilderFactory<StandardFieldConverter<Long>> {

	public LongFieldPredicateBuilderFactory(StandardFieldConverter<Long> converter) {
		super( converter );
	}

	@Override
	public LongMatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LongMatchPredicateBuilder( searchContext, absoluteFieldPath, converter );
	}

	@Override
	public LongRangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LongRangePredicateBuilder( searchContext, absoluteFieldPath, converter );
	}
}
