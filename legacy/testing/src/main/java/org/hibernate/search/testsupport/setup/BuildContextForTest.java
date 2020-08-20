/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.setup;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.LogErrorHandler;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.IndexingMode;

/**
 * A {@code BuildContext} implementation for running tests.
 *
 * @author Hardy Ferentschik
 */
public class BuildContextForTest implements BuildContext {
	private final SearchConfiguration searchConfiguration;

	public BuildContextForTest(SearchConfiguration searchConfiguration) {
		this.searchConfiguration = searchConfiguration;
	}

	@Override
	public ExtendedSearchIntegrator getUninitializedSearchIntegrator() {
		return null;
	}

	@Override
	public IndexingMode getIndexingMode() {
		return IndexingMode.EVENT;
	}

	@Override
	public ServiceManager getServiceManager() {
		return new StandardServiceManager( searchConfiguration, this, Environment.DEFAULT_SERVICES_MAP );
	}

	@Override
	public IndexManagerHolder getAllIndexesManager() {
		return new IndexManagerHolder();
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return new LogErrorHandler();
	}
}


