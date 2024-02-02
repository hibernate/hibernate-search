/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class QueryStringPredicateBaseIT extends AbstractBaseQueryStringPredicateBaseIT<QueryStringPredicateFieldStep<?>> {
	//CHECKSTYLE:ON

	@Override
	QueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
		return f.queryString();
	}

	@Override
	protected String predicateTrait() {
		return "predicate:query-string";
	}
}
