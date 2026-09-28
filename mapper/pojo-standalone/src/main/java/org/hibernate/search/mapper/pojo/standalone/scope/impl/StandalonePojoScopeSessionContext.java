package org.hibernate.search.mapper.pojo.standalone.scope.impl;

import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeSessionContext;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexingSessionContext;

public interface StandalonePojoScopeSessionContext
		extends PojoScopeSessionContext, StandalonePojoMassIndexingSessionContext {

}
