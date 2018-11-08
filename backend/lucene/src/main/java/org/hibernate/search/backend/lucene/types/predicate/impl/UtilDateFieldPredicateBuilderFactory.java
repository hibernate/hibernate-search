/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.converter.impl.UtilDateFieldConverter;

public final class UtilDateFieldPredicateBuilderFactory
		extends AbstractStandardLuceneFieldPredicateBuilderFactory<UtilDateFieldConverter> {

	public UtilDateFieldPredicateBuilderFactory(UtilDateFieldConverter converter) {
		super( converter );
	}

	@Override
	public UtilDateMatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new UtilDateMatchPredicateBuilder( searchContext, absoluteFieldPath, converter );
	}

	@Override
	public UtilDateRangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new UtilDateRangePredicateBuilder( searchContext, absoluteFieldPath, converter );
	}
}
