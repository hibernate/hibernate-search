/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.impl;

import org.hibernate.search.mapper.javabean.model.impl.JavaBeanPojoPathsDefinition;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

class JavaBeanEntityTypeContributor implements PojoTypeMetadataContributor {

	private final PojoRawTypeIdentifier<?> typeIdentifier;
	private final String entityName;

	JavaBeanEntityTypeContributor(PojoRawTypeIdentifier<?> typeIdentifier, String entityName) {
		this.typeIdentifier = typeIdentifier;
		this.entityName = entityName;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( !typeIdentifier.equals( collector.typeIdentifier() ) ) {
			// Entity metadata is not inherited; only contribute it to the exact type.
			return;
		}
		collector.markAsEntity( entityName, new JavaBeanPojoPathsDefinition() );
	}
}
