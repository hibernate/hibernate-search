/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.junit;

import java.util.Properties;

import org.hibernate.search.spi.SearchIntegrator;

import org.junit.Assert;
import org.junit.rules.ExternalResource;

/**
 * Testing SearchFactoryHolder.
 *
 * <p>Automatically retrieves configuration options from the classpath file "/test-defaults.properties".
 *
 * @author Sanne Grinovero
 * @since 4.1
 */
public class SearchFactoryHolder extends ExternalResource {

	private final Class<?>[] entities;
	private final Properties configuration;

	private SearchIntegrator searchIntegrator;

	public SearchFactoryHolder(Class<?>... entities) {
		this.entities = entities;
		this.configuration = new Properties();
	}

	public SearchIntegrator getSearchFactory() {
		return searchIntegrator;
	}

	@Override
	protected void before() throws Throwable {
		searchIntegrator = createSearchFactory();
	}

	private SearchIntegrator createSearchFactory() {
		throw new UnsupportedOperationException( "To be implemented by delegating to Search 6 APIs." );
	}

	@Override
	protected void after() {
		if ( searchIntegrator != null ) {
			try {
				searchIntegrator.close();
			}
			finally {
				searchIntegrator = null;
			}
		}
	}

	public SearchFactoryHolder withProperty(String key, Object value) {
		Assert.assertNull( "SearchIntegrator already initialized", searchIntegrator );
		configuration.put( key, value );
		return this;
	}

}
