/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.spi;

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

	@Override
	void close();

	static Builder builder(SearchIntegrationEnvironment environment) {
		return new SearchIntegrationBuilder( environment );
	}

	interface Builder {

		<PBM extends MappingPartialBuildState> Builder addMappingInitiator(
				MappingKey<PBM, ?> mappingKey, MappingInitiator<?, PBM> initiator);

		SearchIntegrationPartialBuildState prepareBuild();

	}
}
