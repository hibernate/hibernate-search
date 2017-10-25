/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.common.impl.SearchMappingRepositoryBuilderImpl;
import org.hibernate.search.engine.mapper.mapping.MappingKey;

/**
 * @author Yoann Rodiere
 */
public interface SearchMappingRepository extends AutoCloseable {

	<M> M getMapping(MappingKey<M> mappingKey);

	@Override
	void close();

	static SearchMappingRepositoryBuilder builder() {
		return new SearchMappingRepositoryBuilderImpl( ConfigurationPropertySource.empty() );
	}

	static SearchMappingRepositoryBuilder builder(ConfigurationPropertySource propertySource) {
		return new SearchMappingRepositoryBuilderImpl( propertySource );
	}
}
