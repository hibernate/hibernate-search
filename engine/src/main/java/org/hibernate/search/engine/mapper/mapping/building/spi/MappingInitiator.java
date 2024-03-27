/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.mapper.model.spi.TypeMetadataContributorProvider;

/**
 * An object responsible for initiating a mapping by contributing its basic configuration (indexed types, type metadata),
 * then creating the mapper based on the configuration processed by the engine.
 *
 * @param <C> The Java type of type metadata contributors
 * @param <MPBS> The Java type of the partially-built mapping
 */
public interface MappingInitiator<C, MPBS extends MappingPartialBuildState> {

	void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<C> configurationCollector);

	Mapper<MPBS> createMapper(MappingBuildContext buildContext,
			TypeMetadataContributorProvider<C> contributorProvider);

}
