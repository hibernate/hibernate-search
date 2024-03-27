/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface PojoLoadingTypeContextProvider {

	<E> PojoLoadingTypeContext<E> forExactType(PojoRawTypeIdentifier<E> typeIdentifier);

}
