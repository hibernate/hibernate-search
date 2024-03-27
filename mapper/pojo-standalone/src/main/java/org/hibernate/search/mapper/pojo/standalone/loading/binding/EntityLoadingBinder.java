/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
