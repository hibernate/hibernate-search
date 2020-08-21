/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.setup;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.spi.DefaultInstanceInitializer;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * A {@code WorkerBuildContext} implementation for running tests.
 *
 * @author Hardy Ferentschik
 */
public class WorkerBuildContextForTest extends BuildContextForTest implements WorkerBuildContext {

	public WorkerBuildContextForTest(SearchConfiguration searchConfiguration) {
		super( searchConfiguration );
	}

	@Override
	public boolean isTransactionManagerExpected() {
		return false;
	}

	@Override
	public boolean isIndexMetadataComplete() {
		return true;
	}

	@Override
	public boolean isDeleteByTermEnforced() {
		return false;
	}

	@Override
	public boolean isMultitenancyEnabled() {
		return false;
	}

	@Override
	public InstanceInitializer getInstanceInitializer() {
		return DefaultInstanceInitializer.DEFAULT_INITIALIZER;
	}

	@Override
	public boolean enlistWorkerInTransaction() {
		return false;
	}
}


