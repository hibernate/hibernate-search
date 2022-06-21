/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.junit;

import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.migration.V5MigrationStandalonePojoSearchIntegratorAdapter;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Testing SearchFactoryHolder.
 *
 * <p>Automatically retrieves configuration options from the classpath file "/test-defaults.properties".
 *
 * @author Sanne Grinovero
 * @since 4.1
 */
public class SearchFactoryHolder implements TestRule {

	private final V5MigrationHelperEngineSetupHelper setupHelper = V5MigrationHelperEngineSetupHelper.create();

	private final Class<?>[] entities;
	private final Map<String, Object> configuration;

	private SearchMapping mapping;
	private SearchIntegrator searchIntegrator;

	public SearchFactoryHolder(Class<?>... entities) {
		this.entities = entities;
		this.configuration = new HashMap<>();
	}

	public SearchIntegrator getSearchFactory() {
		return searchIntegrator;
	}

	public SearchMapping getMapping() {
		return mapping;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		Statement wrapped = new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try {
					mapping = setupHelper.start()
							.withProperties( configuration )
							.setup( entities );
					searchIntegrator = new V5MigrationStandalonePojoSearchIntegratorAdapter( mapping );
					base.evaluate();
				}
				finally {
					mapping = null;
					searchIntegrator = null;
				}
			}
		};
		return setupHelper.apply( wrapped, description );
	}

	public SearchFactoryHolder withProperty(String key, Object value) {
		assertNull( "Mapping already initialized", mapping );
		configuration.put( key, value );
		return this;
	}
}
