/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi;

public interface PojoAdditionalMetadataCollectorEntityTypeNode extends PojoAdditionalMetadataCollector {

	/**
	 * @param propertyName The name of a property hosting the entity ID.
	 * This ID will be used by default to generate document IDs when no document ID was configured in the mapping.
	 */
	void entityIdPropertyName(String propertyName);

}
