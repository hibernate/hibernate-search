/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;

import org.apache.lucene.search.Query;

public interface LuceneSearchPredicateBuilderFactory
		extends SearchPredicateBuilderFactory<LuceneSearchPredicateCollector> {

	LuceneSearchPredicateBuilder fromLuceneQuery(Query luceneQuery);

}
