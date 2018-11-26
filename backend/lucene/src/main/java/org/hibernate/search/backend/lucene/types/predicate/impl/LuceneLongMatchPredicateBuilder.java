/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneMatchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;

class LuceneLongMatchPredicateBuilder extends AbstractLuceneMatchPredicateBuilder<Long, Long> {

	LuceneLongMatchPredicateBuilder(
			LuceneSearchContext searchContext,
			String absoluteFieldPath, LuceneFieldConverter<?, Long> converter) {
		super( searchContext, absoluteFieldPath, converter );
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		return LongPoint.newExactQuery( absoluteFieldPath, value );
	}
}
