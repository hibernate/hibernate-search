/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface PojoMassIndexingTypeContextProvider {

	<E> PojoMassIndexingIndexedTypeContext<E> indexedForExactType(PojoRawTypeIdentifier<E> typeIdentifier);

	<E> Optional<? extends Set<? extends PojoMassIndexingIndexedTypeContext<? extends E>>> indexedForSuperType(
			PojoRawTypeIdentifier<E> typeIdentifier);

}
