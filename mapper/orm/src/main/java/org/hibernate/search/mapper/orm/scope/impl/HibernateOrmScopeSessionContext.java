/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.scope.impl;

import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingSessionContext;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingSessionContext;
import org.hibernate.search.mapper.orm.spi.BatchSessionContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeSessionContext;

public interface HibernateOrmScopeSessionContext
		extends PojoScopeSessionContext, HibernateOrmLoadingSessionContext, HibernateOrmMassIndexingSessionContext,
		BatchSessionContext {

}
