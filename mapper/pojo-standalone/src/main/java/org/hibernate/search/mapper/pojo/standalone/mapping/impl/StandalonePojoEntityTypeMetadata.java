/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.standalone.logging.impl.Log;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class StandalonePojoEntityTypeMetadata<E> implements PojoTypeMetadataContributor {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	final PojoRawTypeModel<E> type;
	final String entityName;
	final EntityConfigurer<E> configurerOrNull;

	StandalonePojoEntityTypeMetadata(PojoRawTypeModel<E> type,
			String entityName, EntityConfigurer<E> configurerOrNull) {
		this.type = type;
		this.entityName = entityName;
		this.configurerOrNull = configurerOrNull;
	}

	StandalonePojoEntityTypeMetadata<E> mergeWith(StandalonePojoEntityTypeMetadata<?> unknownTypeOther) {
		if ( !type.equals( unknownTypeOther.type ) ) {
			throw log.multipleEntityTypeDefinitions( type );
		}
		@SuppressWarnings("unchecked")
		StandalonePojoEntityTypeMetadata<E> other = (StandalonePojoEntityTypeMetadata<E>) unknownTypeOther;
		if ( !entityName.equals( other.entityName ) ) {
			throw log.multipleEntityTypeDefinitions( type );
		}
		EntityConfigurer<E> configurerOrNull;
		if ( this.configurerOrNull == null ) {
			configurerOrNull = other.configurerOrNull;
		}
		else if ( other.configurerOrNull == null ) {
			configurerOrNull = this.configurerOrNull;
		}
		else {
			throw log.multipleEntityTypeDefinitions( type );
		}
		return new StandalonePojoEntityTypeMetadata<>( type, entityName, configurerOrNull );
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( !type.typeIdentifier().equals( collector.typeIdentifier() ) ) {
			// Entity metadata is not inherited; only contribute it to the exact type.
			return;
		}
		var node = collector.markAsEntity();
		if ( entityName != null ) {
			node.entityName( entityName );
		}
		if ( configurerOrNull != null ) {
			node.loadingBinder( configurerOrNull );
		}
	}
}
