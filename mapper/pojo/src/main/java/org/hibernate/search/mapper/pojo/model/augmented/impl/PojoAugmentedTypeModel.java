/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.augmented.impl;

import java.util.Map;

public class PojoAugmentedTypeModel {
	private final boolean entity;
	private final Map<String, PojoAugmentedPropertyModel> properties;

	public PojoAugmentedTypeModel(boolean entity, Map<String, PojoAugmentedPropertyModel> properties) {
		this.entity = entity;
		this.properties = properties;
	}

	/**
	 * Determine whether the given type is an entity type.
	 * <p>
	 * Types marked as entity types are guaranteed by the augmented model contributors
	 * to be the only types that can be the target of an association.
	 * All other types are assumed to only be able to be embedded in other objects,
	 * with their lifecycle completely tied to their embedding object.
	 * As a result, entity types are the only types whose lifecycle events are expected to be sent
	 * to the POJO workers.
	 *
	 * @return {@code true} if this type is an entity type, {@code false} otherwise.
	 */
	public boolean isEntity() {
		return entity;
	}

	public PojoAugmentedPropertyModel getProperty(String name) {
		return properties.getOrDefault( name, PojoAugmentedPropertyModel.EMPTY );
	}

	public Map<String, PojoAugmentedPropertyModel> getAugmentedProperties() {
		return properties;
	}
}
