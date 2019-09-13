/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.impl;

import java.util.Map;
import java.util.Optional;

public class PojoTypeAdditionalMetadata {
	private final Optional<PojoEntityTypeAdditionalMetadata> entityTypeMetadata;
	private final Map<String, PojoPropertyAdditionalMetadata> propertiesAdditionalMetadata;

	public PojoTypeAdditionalMetadata(Optional<PojoEntityTypeAdditionalMetadata> entityTypeMetadata,
			Map<String, PojoPropertyAdditionalMetadata> propertiesAdditionalMetadata) {
		this.entityTypeMetadata = entityTypeMetadata;
		this.propertiesAdditionalMetadata = propertiesAdditionalMetadata;
	}

	/**
	 * Determine whether the given type is an entity type.
	 * <p>
	 * Types marked as entity types are guaranteed by the contributors
	 * to be the only types that can be the target of an association.
	 * All other types are assumed to only be able to be embedded in other objects,
	 * with their lifecycle completely tied to their embedding object.
	 * As a result, entity types are the only types whose lifecycle events are expected to be sent
	 * to POJO indexing plans.
	 *
	 * @return {@code true} if this type is an entity type, {@code false} otherwise.
	 */
	public boolean isEntity() {
		return entityTypeMetadata.isPresent();
	}

	public Optional<PojoEntityTypeAdditionalMetadata> getEntityTypeMetadata() {
		return entityTypeMetadata;
	}

	public PojoPropertyAdditionalMetadata getPropertyAdditionalMetadata(String name) {
		return propertiesAdditionalMetadata.getOrDefault( name, PojoPropertyAdditionalMetadata.EMPTY );
	}

	public Map<String, PojoPropertyAdditionalMetadata> getPropertiesAdditionalMetadata() {
		return propertiesAdditionalMetadata;
	}
}
