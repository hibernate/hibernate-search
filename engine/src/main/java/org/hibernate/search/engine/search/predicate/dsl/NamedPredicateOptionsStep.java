/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The initial and final step in named predicate definition.
 */
public interface NamedPredicateOptionsStep extends PredicateFinalStep {

	NamedPredicateOptionsStep param(String name, Object value);

}
