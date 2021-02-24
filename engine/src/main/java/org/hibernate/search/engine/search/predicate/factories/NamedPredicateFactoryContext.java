/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.factories;

import org.hibernate.search.engine.search.predicate.dsl.NamedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * The context provided to the {@link NamedPredicateFactory}.
 * @see NamedPredicateFactory#create(NamedPredicateFactoryContext)
 */
public interface NamedPredicateFactoryContext {

	/**
	 * @return A predicate factory. This factory is only valid in the present context and must not be used after {@link NamedPredicateFactory#create(NamedPredicateFactoryContext)} returns.
	 * @see SearchPredicateFactory
	 */
	SearchPredicateFactory predicate();

	/**
	 * @param name a name of parameter
	 * @return parameter of the named predicate factory
	 * @see NamedPredicateOptionsStep#param(java.lang.String, java.lang.Object)
	 */
	Object param(String name);

	/**
	 * @param relative relative name
	 * @return absolute path.
	 */
	String resolvePath(String relative);
}
