/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;

public interface PojoAdditionalMetadataCollectorEntityTypeNode extends PojoAdditionalMetadataCollector {

	/**
	 * @param entityName The name of this entity type.
	 */
	void entityName(String entityName);

	/**
	 * @param secondaryEntityName A secondary name for this entity type,
	 * for instance the "native" Hibernate ORM entity name (generally just the fully qualified class name).
	 * Secondary names may conflict with the primary {@link #entityName(String)}.
	 * The primary name takes precedence in case of lookup by name.
	 */
	void secondaryEntityName(String secondaryEntityName);

	/**
	 * @param pathDefinitionProvider A provider of path definition for this entity type,
	 * i.e. the object supporting the creation of path filters that will be used in particular
	 * when performing dirty checking during automatic reindexing.
	 */
	void pathDefinitionProvider(PojoPathDefinitionProvider pathDefinitionProvider);

	/**
	 * @param propertyName The name of a property hosting the entity ID.
	 * This ID will be used by default to generate document IDs when no document ID was configured in the mapping.
	 */
	void entityIdPropertyName(String propertyName);

}
