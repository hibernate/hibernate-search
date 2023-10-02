/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.backend;

import java.util.Map;

import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;

public class AsyncBackendLongWorkListStressTest extends SyncBackendLongWorkListStressTest {

	@Override
	public void configure(Map<String, Object> cfg) {
		super.configure( cfg );
		cfg.put( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
				AutomaticIndexingSynchronizationStrategyNames.ASYNC );
	}

}
