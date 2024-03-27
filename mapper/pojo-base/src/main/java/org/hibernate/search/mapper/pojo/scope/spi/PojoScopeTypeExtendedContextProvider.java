/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.scope.spi;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface PojoScopeTypeExtendedContextProvider<E, C> {

	C forExactType(PojoRawTypeIdentifier<? extends E> typeIdentifier);

}
