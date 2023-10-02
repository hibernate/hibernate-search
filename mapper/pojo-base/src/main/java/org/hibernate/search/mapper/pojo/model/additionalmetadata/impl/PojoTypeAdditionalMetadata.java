/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class PojoTypeAdditionalMetadata {
	private final Optional<PojoEntityTypeAdditionalMetadata> entityTypeMetadata;
	private final Optional<PojoIndexedTypeAdditionalMetadata> indexedTypeMetadata;
	private final Map<String, Supplier<PojoPropertyAdditionalMetadata>> propertiesAdditionalMetadataSuppliers;
	private final Map<String, PojoPropertyAdditionalMetadata> propertiesAdditionalMetadata = new LinkedHashMap<>();

	public PojoTypeAdditionalMetadata(Optional<PojoEntityTypeAdditionalMetadata> entityTypeMetadata,
			Optional<PojoIndexedTypeAdditionalMetadata> indexedTypeMetadata,
			Map<String, Supplier<PojoPropertyAdditionalMetadata>> propertiesAdditionalMetadataSuppliers) {
		this.entityTypeMetadata = entityTypeMetadata;
		this.indexedTypeMetadata = indexedTypeMetadata;
		this.propertiesAdditionalMetadataSuppliers = propertiesAdditionalMetadataSuppliers;
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

	public Optional<PojoIndexedTypeAdditionalMetadata> getIndexedTypeMetadata() {
		return indexedTypeMetadata;
	}

	public Set<String> getNamesOfPropertiesWithAdditionalMetadata() {
		return propertiesAdditionalMetadataSuppliers.keySet();
	}

	public PojoPropertyAdditionalMetadata getPropertyAdditionalMetadata(String name) {
		return propertiesAdditionalMetadata.computeIfAbsent( name, theName -> {
			Supplier<PojoPropertyAdditionalMetadata> supplier = propertiesAdditionalMetadataSuppliers.get( theName );
			if ( supplier == null ) {
				return PojoPropertyAdditionalMetadata.EMPTY;
			}
			return supplier.get();
		} );
	}
}
