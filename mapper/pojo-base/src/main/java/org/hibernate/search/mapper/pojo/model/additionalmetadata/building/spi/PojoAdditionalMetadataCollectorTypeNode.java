/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public interface PojoAdditionalMetadataCollectorTypeNode extends PojoAdditionalMetadataCollector {

	/**
	 * @return The type metadata is being contributed to.
	 */
	PojoRawTypeModel<?> getType();

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
	 * @return A {@link PojoAdditionalMetadataCollectorEntityTypeNode}, allowing to provide optional metadata
	 * about the entity.
	 */
	PojoAdditionalMetadataCollectorEntityTypeNode markAsEntity(String entityName,
			PojoPathFilterFactory<Set<String>> pathFilterFactory);

	/**
	 * Mark this type as an indexed type.
	 * <p>
	 * <strong>WARNING:</strong> only entity types may be indexed.
	 *
	 * @param backendName The name of the backend where this type should be indexed,
	 * or {@link Optional#empty()} to target the default backend.
	 * @param indexName The name of the index where this type should be indexed,
	 * or {@link Optional#empty()} to derive the index name from the entity type.
	 * @return A {@link PojoAdditionalMetadataCollectorIndexedTypeNode}, allowing to provide optional metadata
	 * about the indexed type.
	 */
	PojoAdditionalMetadataCollectorIndexedTypeNode markAsIndexed(Optional<String> backendName, Optional<String> indexName);

	PojoAdditionalMetadataCollectorPropertyNode property(String propertyName);

}
