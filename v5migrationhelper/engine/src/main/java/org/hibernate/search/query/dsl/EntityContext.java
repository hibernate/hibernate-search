/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateOptionsStep;

/**
 * @author Emmanuel Bernard
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface EntityContext {
	/**
	 * @param field The field name.
	 * @param analyzerName The analyzer name.
	 * @return EntityContext
	 * @deprecated See the deprecation note on {@link QueryBuilder}.
	 * Also, analyzer overrides are done on a per-predicate basis in Hibernate Search 6.
	 * See {@link MatchPredicateOptionsStep#analyzer(String)},
	 * {@link SimpleQueryStringPredicateOptionsStep#analyzer(String)},
	 * etc.
	 */
	@Deprecated
	EntityContext overridesForField(String field, String analyzerName);

	/**
	 * @return the query builder
	 */
	QueryBuilder get();
}
