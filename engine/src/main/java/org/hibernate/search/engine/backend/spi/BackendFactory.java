/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.spi;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.util.common.reporting.EventContext;

public interface BackendFactory {

	/**
	 * @param eventContext An {@link org.hibernate.search.util.common.reporting.EventContext} representing the backend.
	 * @param context The build context.
	 * @param propertySource A configuration property source, appropriately masked so that the backend
	 * doesn't need to care about Hibernate Search prefixes (hibernate.search.*, etc.). All the properties
	 * can be accessed at the root.
	 * <strong>CAUTION:</strong> the property keys listed in {@link BackendSettings},
	 * in particular {@value BackendSettings#TYPE} and {@value BackendSettings#INDEXES}
	 * are reserved for use by the engine.
	 * @return A backend.
	 */
	BackendImplementor create(EventContext eventContext, BackendBuildContext context,
			ConfigurationPropertySource propertySource);

}
