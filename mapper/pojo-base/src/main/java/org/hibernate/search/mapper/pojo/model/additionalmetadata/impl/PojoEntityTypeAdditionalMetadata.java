/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathsDefinition;

public class PojoEntityTypeAdditionalMetadata {
	private final String entityName;
	private final PojoPathsDefinition pathsDefinition;
	private final Optional<String> entityIdPropertyName;

	public PojoEntityTypeAdditionalMetadata(String entityName,
			PojoPathsDefinition pathsDefinition,
			Optional<String> entityIdPropertyName) {
		this.entityName = entityName;
		this.pathsDefinition = pathsDefinition;
		this.entityIdPropertyName = entityIdPropertyName;
	}

	public String getEntityName() {
		return entityName;
	}

	/**
	 * @return A path filter factory for this type.
	 */
	public PojoPathsDefinition getPathsDefinition() {
		return pathsDefinition;
	}

	public Optional<String> getEntityIdPropertyName() {
		return entityIdPropertyName;
	}
}
