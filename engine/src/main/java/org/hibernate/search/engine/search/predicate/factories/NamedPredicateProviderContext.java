/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.factories;

import org.hibernate.search.engine.search.predicate.dsl.NamedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context passed to {@link NamedPredicateProvider#create(NamedPredicateProviderContext)}.
 * @see NamedPredicateProvider#create(NamedPredicateProviderContext)
 */
@Incubating
public interface NamedPredicateProviderContext {

	/**
	 * @return A predicate factory.
	 * This factory is only valid in the present context and must not be used after
	 * {@link NamedPredicateProvider#create(NamedPredicateProviderContext)} returns.
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
	 * @param relativeFieldPath The path a field, relative to the element to which the named predicate was assigned.
	 * @return The absolute path of the field. Note the path is returned even if the field doesn't exist.
	 */
	String absolutePath(String relativeFieldPath);
}
