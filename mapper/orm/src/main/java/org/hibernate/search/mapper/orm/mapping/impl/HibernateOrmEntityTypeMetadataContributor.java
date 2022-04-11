/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.Optional;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmPathsDefinition;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

final class HibernateOrmEntityTypeMetadataContributor implements PojoTypeMetadataContributor {

	private final PojoRawTypeIdentifier<?> typeIdentifier;
	private final PersistentClass persistentClass;
	private final Optional<String> identifierPropertyNameOptional;

	HibernateOrmEntityTypeMetadataContributor(PojoRawTypeIdentifier<?> typeIdentifier,
			PersistentClass persistentClass, Optional<String> identifierPropertyNameOptional) {
		this.typeIdentifier = typeIdentifier;
		this.persistentClass = persistentClass;
		this.identifierPropertyNameOptional = identifierPropertyNameOptional;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( !typeIdentifier.equals( collector.typeIdentifier() ) ) {
			// Entity metadata is not inherited; only contribute it to the exact type.
			return;
		}
		collector.markAsEntity(
				persistentClass.getJpaEntityName(),
				new HibernateOrmPathsDefinition( persistentClass )
		)
				.entityIdPropertyName( identifierPropertyNameOptional.orElse( null ) );
	}
}
