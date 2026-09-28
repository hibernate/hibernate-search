package org.hibernate.search.mapper.orm.massindexing.impl;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;

public interface HibernateOrmMassIndexingSessionContext extends PojoMassIndexingSessionContext {

	SharedSessionContractImplementor sessionImplementor();

}
