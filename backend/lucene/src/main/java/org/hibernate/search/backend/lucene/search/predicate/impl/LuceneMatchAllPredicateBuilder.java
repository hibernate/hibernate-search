/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;


class LuceneMatchAllPredicateBuilder extends AbstractLuceneSearchPredicateBuilder
		implements MatchAllPredicateBuilder {

	LuceneMatchAllPredicateBuilder(LuceneSearchContext searchContext) {
		super( searchContext );
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		// Nothing to do
	}

	@Override
	protected Query doBuild(PredicateRequestContext context) {
		return new MatchAllDocsQuery();
	}
}
