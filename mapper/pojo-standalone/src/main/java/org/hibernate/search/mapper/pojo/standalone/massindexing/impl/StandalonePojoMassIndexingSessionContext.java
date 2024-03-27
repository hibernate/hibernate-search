/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.massindexing.impl;

import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingSessionContext;

public interface StandalonePojoMassIndexingSessionContext
		extends PojoMassIndexingSessionContext, StandalonePojoLoadingSessionContext {

}
