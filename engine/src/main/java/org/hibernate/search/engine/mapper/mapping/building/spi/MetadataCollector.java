/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

/**
 * @author Yoann Rodiere
 */
public interface MetadataCollector {

	<C extends TypeMetadataContributor> void collect(
			MapperFactory<C, ?> mapperFactory, MappableTypeModel typeModel, String indexName, C contributor
	);

}
