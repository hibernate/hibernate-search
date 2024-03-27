/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;

import org.hibernate.search.mapper.pojo.mapping.spi.PojoRawTypeIdentifierResolver;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.data.spi.KeyValueProvider;

public interface PojoWorkTypeContextProvider extends PojoRawTypeIdentifierResolver {

	<E> PojoWorkTypeContext<?, E> forExactType(PojoRawTypeIdentifier<E> typeIdentifier);

	<E> PojoWorkIndexedTypeContext<?, E> indexedForExactType(PojoRawTypeIdentifier<E> typeIdentifier);

	KeyValueProvider<String, ? extends PojoWorkTypeContext<?, ?>> byEntityName();

}
