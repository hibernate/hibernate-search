/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.augmented.building.spi;

import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAugmentedTypeModel;

public interface PojoAugmentedModelCollectorTypeNode extends PojoAugmentedModelCollector {

	/**
	 * Mark this type as an entity type.
	 * <p>
	 * <strong>WARNING:</strong> entity types must always be defined upfront without relying on
	 * {@link org.hibernate.search.engine.mapper.mapping.building.spi.MetadataCollector#collectDiscoverer(org.hibernate.search.v6poc.entity.mapping.building.spi.MapperFactory, org.hibernate.search.v6poc.entity.mapping.building.spi.TypeMetadataDiscoverer) metadata discovery},
	 * because Hibernate Search needs to be able to have a complete view of all the possible entity types
	 * in order to handle automatic reindexing.
	 * Relying on type discovery for entity detection would mean running the risk of one particular
	 * entity subtype not being detected (because only its supertype is mentioned in the schema of indexed entities),
	 * which could result in incomplete automatic reindexing.
	 *
	 * @see PojoAugmentedTypeModel#isEntity()
	 */
	void markAsEntity();

	PojoAugmentedModelCollectorPropertyNode property(String propertyName);

}
