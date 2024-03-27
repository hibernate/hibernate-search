/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingMappingContext;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;

public interface HibernateOrmMassIndexingMappingContext
		extends PojoMassIndexingMappingContext, HibernateOrmLoadingMappingContext {
	TenancyConfiguration tenancyConfiguration();
}
