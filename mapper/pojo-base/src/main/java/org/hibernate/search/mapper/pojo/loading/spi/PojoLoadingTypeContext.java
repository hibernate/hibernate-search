/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface PojoLoadingTypeContext<E> {

	String entityName();

	String secondaryEntityName();

	PojoRawTypeIdentifier<E> typeIdentifier();

	List<PojoRawTypeIdentifier<? super E>> ascendingSuperTypes();

	boolean isSingleConcreteTypeInEntityHierarchy();

	PojoSelectionLoadingStrategy<? super E> selectionLoadingStrategy();

	Optional<PojoSelectionLoadingStrategy<? super E>> selectionLoadingStrategyOptional();

	PojoMassLoadingStrategy<? super E, ?> massLoadingStrategy();

	Optional<PojoMassLoadingStrategy<? super E, ?>> massLoadingStrategyOptional();

	boolean hasNonIndexedConcreteSubtypes();

}
