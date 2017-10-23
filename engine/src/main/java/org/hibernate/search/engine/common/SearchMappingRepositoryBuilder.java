/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common;

import java.util.Properties;

import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;

/**
 * @author Yoann Rodiere
 */
public interface SearchMappingRepositoryBuilder {

	SearchMappingRepositoryBuilder setProperty(String name, String value);

	SearchMappingRepositoryBuilder setProperties(Properties properties);

	SearchMappingRepositoryBuilder addMapping(MetadataContributor mappingContributor);

	SearchMappingRepository build();

	SearchMappingRepository getBuiltResult();

}
