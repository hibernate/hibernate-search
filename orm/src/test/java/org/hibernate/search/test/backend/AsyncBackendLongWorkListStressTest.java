/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend;

import org.hibernate.cfg.Configuration;

public class AsyncBackendLongWorkListStressTest extends SyncBackendLongWorkListStressTest {

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.worker.execution", "async" );
	}
}
