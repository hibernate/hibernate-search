/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;

/**
 * An alias interface used when we don't want to write the full version of
 * {@link PojoMetadataContributor} with generic parameters.
 */
public interface PojoPropertyMetadataContributor
		extends PojoMetadataContributor<PojoAdditionalMetadataCollectorPropertyNode, PojoMappingCollectorPropertyNode> {

}
