/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;

public class PojoEntityTypeAdditionalMetadata {
	private final String entityName;
	private final String secondaryEntityName;
	private final PojoPathDefinitionProvider pathDefinitionProvider;
	private final Optional<String> entityIdPropertyName;

	public PojoEntityTypeAdditionalMetadata(String entityName, String secondaryEntityName,
			PojoPathDefinitionProvider pathDefinitionProvider,
			Optional<String> entityIdPropertyName) {
		this.entityName = entityName;
		this.secondaryEntityName = secondaryEntityName;
		this.pathDefinitionProvider = pathDefinitionProvider;
		this.entityIdPropertyName = entityIdPropertyName;
	}

	public String getEntityName() {
		return entityName;
	}

	public String getSecondaryEntityName() {
		return secondaryEntityName;
	}

	public PojoPathDefinitionProvider pathDefinitionProvider() {
		return pathDefinitionProvider;
	}

	public Optional<String> getEntityIdPropertyName() {
		return entityIdPropertyName;
	}
}
