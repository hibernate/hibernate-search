/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * An object where the clauses and options of a {@link SearchPredicateFactory#nested(String) nested predicate} can be set.
 * <p>
 * Different types of clauses have different effects, see {@link BooleanPredicateOptionsCollector}.
 */
public interface NestedPredicateOptionsCollector<S extends NestedPredicateOptionsCollector<?>>
		extends BooleanPredicateOptionsCollector<S> {

}
