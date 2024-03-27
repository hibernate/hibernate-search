/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.event.impl;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface HibernateOrmListenerTypeContext {

	PojoRawTypeIdentifier<?> typeIdentifier();

	Object toIndexingPlanProvidedId(Object entityId);

	PojoPathFilter dirtyFilter();

	PojoPathFilter dirtyContainingAssociationFilter();

}
