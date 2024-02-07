/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading.binding;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A binder for loading of a specific entity type.
 * <p>
 * If a binder is assigned to a given entity type,
 * it will also apply to its subtypes,
 * except for subtypes that have another binder assigned.
 */
@Incubating
public interface EntityLoadingBinder {

	/**
	 * Binds loading for the entity, using the given {@code context}.
	 * @param context A context exposing methods to bind loading.
	 */
	void bind(EntityLoadingBindingContext context);

}
