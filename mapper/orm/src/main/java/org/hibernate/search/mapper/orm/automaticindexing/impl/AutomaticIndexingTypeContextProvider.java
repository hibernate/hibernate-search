/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface AutomaticIndexingTypeContextProvider {

	<T> AutomaticIndexingIndexedTypeContext indexedForExactType(PojoRawTypeIdentifier<T> typeIdentifier);

}
