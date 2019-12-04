/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;

class LuceneSearchResultImpl<H> extends SimpleSearchResult<H>
		implements LuceneSearchResult<H> {
	LuceneSearchResultImpl(long hitCount, List<H> hits, Map<AggregationKey<?>, ?> aggregationResults,
			Duration took, Boolean timedOut) {
		super( hitCount, hits, aggregationResults, took, timedOut );
	}
}
