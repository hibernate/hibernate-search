/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmPathFilterFactory;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;

final class HibernateOrmEntityTypeMetadataContributor implements PojoTypeMetadataContributor {

	private final PersistentClass persistentClass;
	private final String idPropertyName;

	HibernateOrmEntityTypeMetadataContributor(PersistentClass persistentClass, String idPropertyName) {
		this.persistentClass = persistentClass;
		this.idPropertyName = idPropertyName;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( !persistentClass.getMappedClass().equals( collector.getType().getJavaClass() ) ) {
			// Entity metadata is not inherited; only contribute it to the exact type.
			return;
		}
		collector.markAsEntity( new HibernateOrmPathFilterFactory( persistentClass ) )
				.entityIdPropertyName( idPropertyName );
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		// Nothing to do
	}
}
