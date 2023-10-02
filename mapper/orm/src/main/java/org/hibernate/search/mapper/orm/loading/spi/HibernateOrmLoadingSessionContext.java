/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.spi;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingSessionContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;

public interface HibernateOrmLoadingSessionContext extends PojoLoadingSessionContext, PojoMassIndexingSessionContext {

	SessionImplementor session();

}
