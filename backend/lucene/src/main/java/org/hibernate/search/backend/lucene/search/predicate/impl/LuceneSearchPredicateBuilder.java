/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

import org.apache.lucene.search.Query;

public interface LuceneSearchPredicateBuilder {

	SearchPredicate build();

	// TODO HSEARCH-3476 this is just a temporary hack:
	//  we should have one SearchPredicate implementation per type of predicate,
	//  and move this method there.
	void checkNestableWithin(String expectedParentNestedPath);

	// TODO HSEARCH-3476 this is just a temporary hack:
	//  we should have one SearchPredicate implementation per type of predicate,
	//  and move this method there.
	Query toQuery(PredicateRequestContext context);

}
