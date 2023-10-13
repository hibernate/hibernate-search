/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;

public abstract class TckBackendSetupStrategy<C extends BackendConfiguration> {

	protected final C backendConfiguration;
	protected final Map<String, Object> properties = new LinkedHashMap<>();
	private boolean expectCustomBeans = false;

	public TckBackendSetupStrategy(C backendConfiguration) {
		this.backendConfiguration = backendConfiguration;
		properties.putAll( backendConfiguration.rawBackendProperties() );
	}

	public Map<String, ?> createBackendConfigurationProperties(TestConfigurationProvider configurationProvider) {
		return configurationProvider.interpolateProperties( properties );
	}

	public SearchSetupHelper.SetupContext startSetup(SearchSetupHelper.SetupContext setupContext) {
		if ( expectCustomBeans ) {
			setupContext = setupContext.expectCustomBeans();
		}
		return setupContext;
	}

	public abstract TckBackendAccessor createBackendAccessor(TestConfigurationProvider configurationProvider);

	public TckBackendSetupStrategy<C> setProperty(String key, Object value) {
		properties.put( key, value );
		return this;
	}

	public TckBackendSetupStrategy<C> expectCustomBeans() {
		expectCustomBeans = true;
		return this;
	}

}
