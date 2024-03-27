/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.scope.spi;

import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReferenceFactoryDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoRawTypeIdentifierResolver;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkMappingContext;

/**
 * Mapping-scoped information and operations for use in POJO scopes.
 */
public interface PojoScopeMappingContext extends PojoWorkMappingContext, PojoMassIndexingMappingContext {

	PojoRawTypeIdentifierResolver typeIdentifierResolver();

	PojoEntityReferenceFactoryDelegate entityReferenceFactoryDelegate();

}
