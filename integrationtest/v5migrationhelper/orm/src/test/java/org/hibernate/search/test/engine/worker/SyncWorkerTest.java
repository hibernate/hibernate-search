/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.engine.worker;

import java.util.Map;

import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;

/**
 * @author Emmanuel Bernard
 */
public class SyncWorkerTest extends WorkerTestCase {

	@Override
	public void configure(Map<String, Object> cfg) {
		cfg.put( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
				AutomaticIndexingSynchronizationStrategyNames.SYNC );
	}

}
