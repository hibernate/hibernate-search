/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.spi;

import java.util.Optional;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.common.impl.SearchIntegrationBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingInitiator;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;

public interface SearchIntegration extends AutoCloseable {

	Backend backend();

	Backend backend(String backendName);

	IndexManager indexManager(String indexManagerName);

	Builder restartBuilder(SearchIntegrationEnvironment environment);

	@Override
	void close();

	static Builder builder(SearchIntegrationEnvironment environment) {
		return new SearchIntegrationBuilder( environment, Optional.empty() );
	}

	interface Builder {

		<PBM extends MappingPartialBuildState> Builder addMappingInitiator(
				MappingKey<PBM, ?> mappingKey, MappingInitiator<?, PBM> initiator);

		SearchIntegrationPartialBuildState prepareBuild();

	}

	interface Handle {

		/**
		 * @return The {@link SearchIntegration}, if available.
		 * @throws org.hibernate.search.util.common.SearchException If the {@link SearchIntegration} hasn't been completely started yet.
		 */
		SearchIntegration getOrFail();

		/**
		 * @return The {@link SearchIntegration}, if available, or {@code null}.
		 */
		SearchIntegration getOrNull();

	}
}
