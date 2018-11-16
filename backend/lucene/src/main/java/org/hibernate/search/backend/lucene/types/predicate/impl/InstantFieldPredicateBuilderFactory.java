/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.converter.impl.InstantFieldConverter;

public final class InstantFieldPredicateBuilderFactory
		extends AbstractStandardLuceneFieldPredicateBuilderFactory<InstantFieldConverter> {

	public InstantFieldPredicateBuilderFactory(InstantFieldConverter converter) {
		super( converter );
	}

	@Override
	public InstantMatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new InstantMatchPredicateBuilder( searchContext, absoluteFieldPath, converter );
	}

	@Override
	public InstantRangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new InstantRangePredicateBuilder( searchContext, absoluteFieldPath, converter );
	}
}
