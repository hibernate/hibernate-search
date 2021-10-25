/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorEntityTypeNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoEntityTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathsDefinition;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PojoEntityTypeAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorEntityTypeNode {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String entityName;
	private final PojoPathsDefinition pathsDefinition;
	private String entityIdPropertyName;

	PojoEntityTypeAdditionalMetadataBuilder(String entityName, PojoPathsDefinition pathsDefinition) {
		this.entityName = entityName;
		this.pathsDefinition = pathsDefinition;
	}

	void checkSameEntity(String entityName) {
		if ( this.entityName.equals( entityName ) ) {
			return;
		}
		throw log.multipleEntityNames(
				this.entityName,
				entityName
		);
	}

	@Override
	public void entityIdPropertyName(String propertyName) {
		this.entityIdPropertyName = propertyName;
	}

	public PojoEntityTypeAdditionalMetadata build() {
		return new PojoEntityTypeAdditionalMetadata(
				entityName,
				pathsDefinition,
				Optional.ofNullable( entityIdPropertyName )
		);
	}
}
