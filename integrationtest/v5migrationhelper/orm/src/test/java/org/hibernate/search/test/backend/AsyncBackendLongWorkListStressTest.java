/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
