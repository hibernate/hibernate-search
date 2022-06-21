/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.metadata;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A provider of configuration for a specific entity type,
 * for example loading.
 * <p>
 * If a configurer is assigned to a given entity type,
 * it will also apply to its subtypes,
 * but its configuration can be overridden in a subtype by assigning another configurer to that subtype.
 */
@Incubating
public interface EntityConfigurer<E> {

	/**
	 * Configures the entity, in particular loading, using the given {@code context}.
	 * @param context A context exposing methods to configure the entity.
	 */
	void configure(EntityConfigurationContext<E> context);

}
