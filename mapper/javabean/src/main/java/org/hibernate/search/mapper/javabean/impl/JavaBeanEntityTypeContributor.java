/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.impl;

import org.hibernate.search.mapper.javabean.model.impl.JavaBeanSimpleStringSetPojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class JavaBeanEntityTypeContributor implements PojoTypeMetadataContributor {

	private final PojoRawTypeModel<?> typeModel;
	private final String entityName;

	JavaBeanEntityTypeContributor(PojoRawTypeModel<?> typeModel, String entityName) {
		this.typeModel = typeModel;
		this.entityName = entityName;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		try {
			if ( !typeModel.equals( collector.getType() ) ) {
				// Entity metadata is not inherited; only contribute it to the exact type.
				return;
			}
			collector.markAsEntity( entityName, new JavaBeanSimpleStringSetPojoPathFilterFactory() );
		}
		catch (RuntimeException e) {
			collector.getFailureCollector().add( e );
		}
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		// Nothing to do
	}
}
