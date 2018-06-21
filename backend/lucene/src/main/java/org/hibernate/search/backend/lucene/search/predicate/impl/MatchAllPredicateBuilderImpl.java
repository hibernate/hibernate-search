/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;

/**
 * @author Guillaume Smet
 */
class MatchAllPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements MatchAllPredicateBuilder<LuceneSearchPredicateContext, LuceneSearchPredicateCollector> {

	@Override
	protected Query buildQuery() {
		return new MatchAllDocsQuery();
	}
}
