/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.converter.impl.LocalDateFieldConverter;

public final class LocalDateFieldPredicateBuilderFactory
		extends AbstractStandardLuceneFieldPredicateBuilderFactory<LocalDateFieldConverter> {

	public LocalDateFieldPredicateBuilderFactory(LocalDateFieldConverter converter) {
		super( converter );
	}

	@Override
	public LocalDateMatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LocalDateMatchPredicateBuilder( searchContext, absoluteFieldPath, converter );
	}

	@Override
	public LocalDateRangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LocalDateRangePredicateBuilder( searchContext, absoluteFieldPath, converter );
	}
}
