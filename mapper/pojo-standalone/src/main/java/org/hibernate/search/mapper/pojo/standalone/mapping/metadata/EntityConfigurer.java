/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.metadata;

import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A provider of configuration for a specific entity type,
 * for example loading.
 * <p>
 * If a configurer is assigned to a given entity type,
 * it will also apply to its subtypes,
 * except for subtypes that have another configurer assigned.
 *
 * @deprecated Implement {@link EntityLoadingBinder} instead.
 */
@Incubating
@Deprecated
public interface EntityConfigurer<E> {

	/**
	 * Configures the entity, in particular loading, using the given {@code context}.
	 * @param context A context exposing methods to configure the entity.
	 */
	void configure(EntityConfigurationContext<E> context);

}
