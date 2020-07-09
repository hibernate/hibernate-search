/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.spi;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.common.impl.SearchIntegrationBuilderImpl;

public interface SearchIntegration extends AutoCloseable {

	Backend backend();

	Backend backend(String backendName);

	IndexManager indexManager(String indexManagerName);

	@Override
	void close();

	static SearchIntegrationBuilder builder(ConfigurationPropertySource propertySource,
			ConfigurationPropertyChecker propertyChecker) {
		return new SearchIntegrationBuilderImpl( propertySource, propertyChecker );
	}
}
