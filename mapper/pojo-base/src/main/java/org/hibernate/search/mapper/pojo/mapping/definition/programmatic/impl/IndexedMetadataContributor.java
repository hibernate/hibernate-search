/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;


class IndexedMetadataContributor implements PojoTypeMetadataContributor {

	private final PojoRawTypeModel<?> typeModel;

	private final String backendName;
	private final String indexName;

	IndexedMetadataContributor(PojoRawTypeModel<?> typeModel,
			String backendName, String indexName) {
		this.typeModel = typeModel;
		this.backendName = backendName;
		this.indexName = indexName;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( !typeModel.equals( collector.getType() ) ) {
			// Index mapping is not inherited; only contribute it to the exact type.
			return;
		}
		collector.markAsIndexed(
				Optional.ofNullable( backendName ),
				Optional.ofNullable( indexName )
		);
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		// Nothing to do
	}

}
