/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.spi;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.cfg.spi.ScopedConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;

public interface SearchIntegrationPartialBuildState {

	/**
	 * Close the resources held by this object (backends, index managers, ...).
	 * <p>
	 * To be called in the event of a failure that will prevent the integration from being finalized.
	 */
	void closeOnFailure();

	/**
	 * @return The bean resolver used in the first phase of the integration.
	 */
	BeanResolver beanResolver();

	/**
	 * @param propertySource The configuration property source,
	 * which may hold additional configuration compared to the environment passed to
	 * {@link SearchIntegration#builder(SearchIntegrationEnvironment)}.
	 * @param propertyChecker The configuration property checker
	 * tracking the given {@code configurationPropertySource}.
	 * @return An object allowing the finalization of the search integration.
	 */
	SearchIntegrationFinalizer finalizer(ScopedConfigurationPropertySource propertySource,
			ConfigurationPropertyChecker propertyChecker);

}
