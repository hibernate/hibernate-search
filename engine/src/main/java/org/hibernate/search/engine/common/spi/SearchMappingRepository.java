/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.spi;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.impl.SearchMappingRepositoryBuilderImpl;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;

public interface SearchMappingRepository extends AutoCloseable {

	<M> M getMapping(MappingKey<M> mappingKey);

	Backend getBackend(String backendName);

	@Override
	void close();

	static SearchMappingRepositoryBuilder builder() {
		return new SearchMappingRepositoryBuilderImpl( ConfigurationPropertySource.empty() );
	}

	static SearchMappingRepositoryBuilder builder(ConfigurationPropertySource propertySource) {
		return new SearchMappingRepositoryBuilderImpl( propertySource );
	}
}
