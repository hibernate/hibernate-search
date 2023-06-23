/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
