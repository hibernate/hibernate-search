/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi;

import java.util.Set;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface PojoAdditionalMetadataCollectorTypeNode extends PojoAdditionalMetadataCollector {

	/**
	 * @return The identifier of the type to which metadata is being contributed.
	 */
	PojoRawTypeIdentifier<?> typeIdentifier();

	/**
	 * Mark this type as an entity type.
	 * <p>
	 * <strong>WARNING:</strong> entity types must always be defined upfront without relying on
	 * {@link MappingConfigurationCollector#collectDiscoverer(org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataDiscoverer) metadata discovery},
	 * because Hibernate Search needs to be able to have a complete view of all the possible entity types
	 * in order to handle automatic reindexing.
	 * Relying on type discovery for entity detection would mean running the risk of one particular
	 * entity subtype not being detected (because only its supertype is mentioned in the schema of indexed entities),
	 * which could result in incomplete automatic reindexing.
	 *
	 * @see PojoTypeAdditionalMetadata#isEntity()
	 *
	 * @param entityName The name of this entity type.
	 * @param pathFilterFactory The path filter factory for this entity type,
	 * i.e. the object allowing to create path filters that will be used in particular
	 * when performing dirty checking during automatic reindexing.
	 * @return A {@link PojoAdditionalMetadataCollectorEntityTypeNode}, to provide optional metadata
	 * about the entity.
	 */
	PojoAdditionalMetadataCollectorEntityTypeNode markAsEntity(String entityName,
			PojoPathFilterFactory<Set<String>> pathFilterFactory);

	/**
	 * Mark this type as an indexed type.
	 * <p>
	 * <strong>WARNING:</strong> only entity types may be indexed.
	 *
	 * @return A {@link PojoAdditionalMetadataCollectorIndexedTypeNode}, to provide optional metadata
	 * about the indexed type.
	 */
	PojoAdditionalMetadataCollectorIndexedTypeNode markAsIndexed();

	PojoAdditionalMetadataCollectorPropertyNode property(String propertyName);

}
