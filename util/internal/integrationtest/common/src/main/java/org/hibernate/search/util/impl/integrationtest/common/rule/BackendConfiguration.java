/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.rules.TestRule;

public abstract class BackendConfiguration {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String BACKEND_TYPE_PROPERTY_KEY = "org.hibernate.search.integrationtest.backend.type";

	// Uncomment one of the following lines to set the backend type when running tests from the IDE
	public static final String IDE_BACKEND_TYPE = "lucene";
	//	public static final String IDE_BACKEND_TYPE = "elasticsearch";

	public static final String BACKEND_TYPE;
	public static final boolean IS_IDE;
	static {
		String property = System.getProperty( BACKEND_TYPE_PROPERTY_KEY );
		if ( property == null ) {
			BACKEND_TYPE = IDE_BACKEND_TYPE;
			IS_IDE = true;
			log.warn( "The backend type wasn't set; tests are probably running from an IDE."
					+ " Defaulting to backend type '" + BACKEND_TYPE + "' and setting it explicitly"
					+ " to avoid problems with classpaths containing multiple backend types." );
			log.warn( "To test another backend type, change the constant 'IDE_BACKEND_TYPE' in class '"
					+ BackendConfiguration.class.getName() + "'." );
			log.warn( "Tests of the backend type auto-detection feature will not work properly." );
		}
		else {
			BACKEND_TYPE = property;
			IS_IDE = false;
		}
	}

	public static boolean isElasticsearch() {
		return "elasticsearch".equals( BACKEND_TYPE );
	}

	public static boolean isLucene() {
		return "lucene".equals( BACKEND_TYPE );
	}

	public Optional<TestRule> testRule() {
		return Optional.empty();
	}

	public <C extends MappingSetupHelper<C, ?, ?, ?>.AbstractSetupContext> C setup(C setupContext,
			String backendNameOrNull, TestConfigurationProvider configurationProvider) {
		setupContext = setupContext
				.withBackendProperties( backendNameOrNull, backendProperties( configurationProvider ) );

		return setupContext;
	}

	public final Map<String, String> backendProperties(TestConfigurationProvider configurationProvider) {
		Map<String, String> rawBackendProperties = rawBackendProperties();
		if ( IS_IDE ) {
			// More than one backend type in the classpath, we have to set it explicitly.
			rawBackendProperties.put( BackendSettings.TYPE, BACKEND_TYPE );
		}
		return configurationProvider.interpolateProperties( rawBackendProperties );
	}

	public abstract Map<String, String> rawBackendProperties();

	public abstract boolean supportsExplicitPurge();

	public abstract boolean supportsExplicitRefresh();

}
