/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.impl.BeanResolverImpl;
import org.hibernate.search.engine.environment.classpath.spi.AggregatedClassLoader;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.DefaultClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.DefaultServiceResolver;
import org.hibernate.search.engine.environment.classpath.spi.ServiceResolver;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class TestConfigurationProvider
		implements BeforeAllCallback, AfterAllCallback,
		BeforeEachCallback, AfterEachCallback {

	private static final String STARTUP_TIMESTAMP = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss.SSS", Locale.ROOT )
			.format( new Date() );

	private String testId;

	@Override
	public void afterAll(ExtensionContext context) {
		afterEach( context );
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		beforeEach( context );
	}

	@Override
	public void afterEach(ExtensionContext context) {
		testId = null;
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		testId = UUID.randomUUID().toString();
	}

	public BeanResolver createBeanResolverForTest() {
		return createBeanResolverForTest( ConfigurationPropertySource.empty() );
	}

	public BeanResolver createBeanResolverForTest(ConfigurationPropertySource configurationPropertySource) {
		AggregatedClassLoader aggregatedClassLoader = AggregatedClassLoader.createDefault();
		ClassResolver classResolver = DefaultClassResolver.create( aggregatedClassLoader );
		ServiceResolver serviceResolver = DefaultServiceResolver.create( aggregatedClassLoader );
		return BeanResolverImpl.create( classResolver, serviceResolver, null,
				configurationPropertySource );
	}

	public Map<String, Object> getPropertiesFromFile(String propertyFilePath) {
		Properties properties = new Properties();
		try ( InputStream propertiesInputStream = getClass().getResourceAsStream( propertyFilePath ) ) {
			if ( propertiesInputStream == null ) {
				throw new IllegalStateException( "Missing test properties file in the classpath: " + propertyFilePath );
			}
			properties.load( propertiesInputStream );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Error loading test properties file: " + propertyFilePath, e );
		}

		Map<String, Object> propertiesAsMap = new LinkedHashMap<>();
		properties.forEach( (k, v) -> {
			if ( k instanceof String ) {
				propertiesAsMap.put( (String) k, v );
			}
		} );

		return interpolateProperties( propertiesAsMap );
	}

	@SuppressWarnings("unchecked")
	public <V> Map<String, V> interpolateProperties(Map<String, V> properties) {
		Map<String, V> interpolatedProperties = new LinkedHashMap<>();

		properties.forEach( (k, v) -> {
			if ( v instanceof String ) {
				interpolatedProperties.put(
						k,
						(V) ( (String) v ).replace( "#{test.id}", testId )
								.replace( "#{test.startup.timestamp}", STARTUP_TIMESTAMP ) );
			}
			else {
				interpolatedProperties.put( k, v );
			}
		} );

		return interpolatedProperties;
	}
}
