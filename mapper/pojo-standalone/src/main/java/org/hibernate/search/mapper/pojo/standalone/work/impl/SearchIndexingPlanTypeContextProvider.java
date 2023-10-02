/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.work.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface SearchIndexingPlanTypeContextProvider {

	<T> SearchIndexingPlanTypeContext<T> forExactClass(Class<T> typeIdentifier);

	<T> SearchIndexingPlanTypeContext<T> forExactType(PojoRawTypeIdentifier<T> typeIdentifier);

}
