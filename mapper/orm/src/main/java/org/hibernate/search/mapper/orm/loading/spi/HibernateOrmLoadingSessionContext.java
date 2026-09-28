package org.hibernate.search.mapper.orm.loading.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingSessionContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;

public interface HibernateOrmLoadingSessionContext extends PojoLoadingSessionContext, PojoMassIndexingSessionContext {

	SharedSessionContractImplementor sessionImplementor();

}
