/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.factories;

import java.util.Collection;
import org.hibernate.search.engine.search.predicate.dsl.FilterPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * The context provided to the filter factory {@link SearchPredicateFactory#def(String)} method.
 */
public interface FilterFactoryContext {

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the bridged POJO type.
	 * Usual compatibility policies do not apply: incompatible changes may be introduced in any future release.
	 */
	SearchPredicateFactory predicate();

	/**
	 * @param <T> type of the parameters
	 * @param name a name of parametr
	 * @return parametr of the filter factory {@link FilterPredicateOptionsStep#param(String)}.
	 */
	<T> T param(String name);

	/**
	 * @return parent path.
	 */
	String getParentPath();

	/**
	 * @return nested path.
	 */
	String getNestedPath();

	/**
	 * @return absolute path.
	 */
	String getAbsolutePath();

	/**
	 * @return names parametr of the filter {@link FilterPredicateOptionsStep#param(String)}.
	 */
	Collection<String> getParamNames();

	String resolvePath(String relativeFieldName);
}
