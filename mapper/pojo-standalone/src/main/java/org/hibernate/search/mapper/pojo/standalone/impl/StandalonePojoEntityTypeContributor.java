/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.impl;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.standalone.model.impl.StandalonePojoPathsDefinition;

class StandalonePojoEntityTypeContributor implements PojoTypeMetadataContributor {

	private final PojoRawTypeIdentifier<?> typeIdentifier;
	private final String entityName;

	StandalonePojoEntityTypeContributor(PojoRawTypeIdentifier<?> typeIdentifier, String entityName) {
		this.typeIdentifier = typeIdentifier;
		this.entityName = entityName;
	}

	@Override
	// Keeping the deprecated form in order to test that it works correctly (for Infinispan in particular)
	@SuppressWarnings("deprecation")
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( !typeIdentifier.equals( collector.typeIdentifier() ) ) {
			// Entity metadata is not inherited; only contribute it to the exact type.
			return;
		}
		collector.markAsEntity( entityName, new StandalonePojoPathsDefinition() );
	}
}
