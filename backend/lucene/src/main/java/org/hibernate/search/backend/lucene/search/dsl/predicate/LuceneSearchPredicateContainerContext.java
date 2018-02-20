/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.predicate;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateFieldSetContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;

/**
 * A DSL context allowing to specify the type of a predicate, with some Lucene-specific methods.
 *
 * @param <N> The type of the next context (returned by terminal calls such as {@link BooleanJunctionPredicateContext#end()}
 * or {@link MatchPredicateFieldSetContext#matching(Object)}).
 */
public interface LuceneSearchPredicateContainerContext<N> extends SearchPredicateContainerContext<N> {

	N fromLuceneQuery(Query query);
}
