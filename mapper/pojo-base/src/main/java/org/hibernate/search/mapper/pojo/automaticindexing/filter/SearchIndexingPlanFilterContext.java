/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.filter;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A context that helps with the indexing filter configuration.
 * <p>
 * Note that only indexed types can be passed to this context's methods. Passing any other types will lead to an
 * exception being thrown. Methods only accept classes and not interfaces.
 * <p>
 * Include/exclude rules work as follows:
 * <ul>
 *     <li>If the type {@code A} is explicitly included by the filter, then a change to an object that is exactly of an indexed type {@code A} is processed.</li>
 *     <li>If the type {@code A} is explicitly excluded by the filter, then a change to an object that is exactly of an indexed type {@code A} is ignored.</li>
 *     <li>
 *         If the type {@code A} is explicitly included by the filter, then a change to an object that is exactly of an indexed type {@code B},
 *         which is a subtype of the type {@code A},
 *         is processed unless the filter explicitly excludes a more specific supertype of a type {@code B}.
 *     </li>
 *     <li>
 *         If the type {@code A} is excluded by the filter explicitly, then a change to an object that is exactly of an indexed type {@code B},
 *         which is a subtype of the type {@code A},
 *         is ignored unless the filter explicitly includes a more specific supertype of a type {@code B}.
 *     </li>
 *     <li>On an attempt to both include and exclude the same type {@code A} an exception will be thrown.</li>
 *     <li>By default, types are included unless any of previous include/exclude rules apply</li>
 * </ul>
 */
@Incubating
public interface SearchIndexingPlanFilterContext {

	/**
	 * Specify an indexed type to include, along with (unless specified otherwise) all its subtypes.
	 *
	 * @param name The name of an indexed type.
	 * @return The same context, for chained calls.
	 *
	 * @see Indexed#index()
	 */
	SearchIndexingPlanFilterContext include(String name);

	/**
	 * Specify an indexed type to include, along with (unless specified otherwise) all its subtypes.
	 *
	 * @param clazz The class of an indexed type.
	 * @return The same context, for chained calls.
	 */
	SearchIndexingPlanFilterContext include(Class<?> clazz);

	/**
	 * Specify an indexed type to exclude, along with (unless specified otherwise) all its subtypes.
	 *
	 * @param name The name of an indexed type.
	 * @return The same context, for chained calls.
	 *
	 * @see Indexed#index()
	 */
	SearchIndexingPlanFilterContext exclude(String name);

	/**
	 * Specify an indexed type to exclude, along with (unless specified otherwise) all its subtypes.
	 *
	 * @param clazz The class of an indexed type.
	 * @return The same context, for chained calls.
	 */
	SearchIndexingPlanFilterContext exclude(Class<?> clazz);

}
