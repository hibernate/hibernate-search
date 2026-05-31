/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;
import org.hibernate.search.util.common.annotation.Incubating;

public interface SearchIntegrationPartialBuildState {

	/**
	 * Close the resources held by this object (backends, index managers, ...).
	 * <p>
	 * To be called in the event of a failure that will prevent the integration from being finalized.
	 */
	void closeOnFailure();

	/**
	 * @return The bean resolver used in the first phase of the integration.
	 */
	BeanResolver beanResolver();

	/**
	 * @param propertySource The configuration property source,
	 * which may hold additional configuration compared to the environment passed to
	 * {@link SearchIntegration#builder(SearchIntegrationEnvironment)}.
	 * @param propertyChecker The configuration property checker
	 * tracking the given {@code configurationPropertySource}.
	 * @return An object allowing the finalization of the search integration.
	 */
	SearchIntegrationFinalizer finalizer(ConfigurationPropertySource propertySource,
			ConfigurationPropertyChecker propertyChecker);

	/**
	 * @param mappingKey The mapping key to look up.
	 * @return The partial build state for the given mapping key.
	 * @param <PBM> The type of the partial build state.
	 */
	@Incubating
	<PBM extends MappingPartialBuildState> PBM mappingPartialBuildState(MappingKey<PBM, ?> mappingKey);

}
