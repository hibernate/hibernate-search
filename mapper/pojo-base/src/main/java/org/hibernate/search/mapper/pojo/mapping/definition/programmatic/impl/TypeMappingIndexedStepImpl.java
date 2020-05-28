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
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingIndexedStep;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;


class TypeMappingIndexedStepImpl implements TypeMappingIndexedStep, PojoTypeMetadataContributor {

	private final PojoRawTypeIdentifier<?> typeIdentifier;

	private String backendName;
	private String indexName;

	TypeMappingIndexedStepImpl(PojoRawTypeIdentifier<?> typeIdentifier) {
		this.typeIdentifier = typeIdentifier;
	}

	@Override
	public TypeMappingIndexedStep backend(String backendName) {
		this.backendName = backendName;
		return this;
	}

	@Override
	public TypeMappingIndexedStep index(String indexName) {
		this.indexName = indexName;
		return this;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( !typeIdentifier.equals( collector.typeIdentifier() ) ) {
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
