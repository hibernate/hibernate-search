/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

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
