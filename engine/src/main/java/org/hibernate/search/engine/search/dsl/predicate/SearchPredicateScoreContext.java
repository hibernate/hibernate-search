/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * A superinterface for contexts allowing to set the score of a predicate to a constant value.
 *
 * @param <S> The "self" type (the actual type of this context)
 */
public interface SearchPredicateScoreContext<S> extends SearchPredicateBoostContext<S> {

	S withConstantScore();

}
