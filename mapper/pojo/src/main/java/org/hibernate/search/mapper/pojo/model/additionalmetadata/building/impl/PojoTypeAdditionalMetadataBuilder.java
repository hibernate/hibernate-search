/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoPropertyAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;

class PojoTypeAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorTypeNode {
	private boolean entity;
	private final Map<String, PojoPropertyAdditionalMetadataBuilder> propertyBuilders = new HashMap<>();

	@Override
	public void markAsEntity() {
		this.entity = true;
	}

	@Override
	public PojoAdditionalMetadataCollectorPropertyNode property(String propertyName) {
		return propertyBuilders.computeIfAbsent( propertyName, ignored -> new PojoPropertyAdditionalMetadataBuilder() );
	}

	public PojoTypeAdditionalMetadata build() {
		Map<String, PojoPropertyAdditionalMetadata> properties = new HashMap<>();
		for ( Map.Entry<String, PojoPropertyAdditionalMetadataBuilder> entry : propertyBuilders.entrySet() ) {
			properties.put( entry.getKey(), entry.getValue().build() );

		}
		return new PojoTypeAdditionalMetadata( entity, properties );
	}
}
