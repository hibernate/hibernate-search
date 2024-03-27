/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.metadata;

import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBindingContext;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A context allowing the definition of entity configuration.
 *
 * @see EntityConfigurer
 *
 * @deprecated Implement {@link EntityLoadingBinder} and use {@link EntityLoadingBindingContext} instead.
 */
@Incubating
@Deprecated
public interface EntityConfigurationContext<E> {

	/**
	 * @param strategy The strategy for selection loading, used in particular during search.
	 */
	void selectionLoadingStrategy(SelectionLoadingStrategy<? super E> strategy);

	/**
	 * @param strategy The strategy for mass loading, used in particular during mass indexing.
	 */
	void massLoadingStrategy(MassLoadingStrategy<? super E, ?> strategy);

}
