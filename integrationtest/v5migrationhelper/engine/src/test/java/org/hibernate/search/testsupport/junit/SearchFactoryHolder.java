/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.testsupport.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.migration.V5MigrationStandalonePojoSearchIntegratorAdapter;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Testing SearchFactoryHolder.
 *
 * <p>Automatically retrieves configuration options from the classpath file "/test-defaults.properties".
 *
 * @author Sanne Grinovero
 * @since 4.1
 */
public class SearchFactoryHolder implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

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

	public SearchFactoryHolder withProperty(String key, Object value) {
		assertThat( mapping ).as( "Mapping already initialized" ).isNotNull();
		configuration.put( key, value );
		return this;
	}

	@Override
	public void afterEach(ExtensionContext extensionContext) throws Exception {
		mapping = null;
		searchIntegrator = null;
		setupHelper.afterAll( extensionContext );
	}

	@Override
	public void beforeAll(ExtensionContext extensionContext) {
		throw new IllegalStateException(
				"SearchFactoryHolder is only available as nonstatic extension, i.e. @RegisterExtension SearchFactoryHolder searchFactoryHolder = new SearchFactoryHolder();" );
	}

	@Override
	public void beforeEach(ExtensionContext extensionContext) throws Exception {
		setupHelper.beforeEach( extensionContext );

		mapping = setupHelper.start()
				.withProperties( configuration )
				.setup( entities );
		searchIntegrator = new V5MigrationStandalonePojoSearchIntegratorAdapter( mapping );
	}
}
