/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.impl.ConfiguredBeanResolver;
import org.hibernate.search.engine.environment.bean.spi.ReflectionBeanProvider;
import org.hibernate.search.engine.environment.classpath.spi.AggregatedClassLoader;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.DefaultClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.DefaultServiceResolver;
import org.hibernate.search.engine.environment.classpath.spi.ServiceResolver;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public final class TestConfigurationProvider implements TestRule {

	private static final String STARTUP_TIMESTAMP = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss.SSS", Locale.ROOT )
			.format( new Date() );

	private String testId;

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				testId = description.getDisplayName().replaceAll( "[^A-Za-z0-9_+().\\[\\]=]+", "_" );
				try {
					base.evaluate();
				}
				finally {
					testId = null;
				}
			}
		};
	}

	public BeanResolver createBeanResolverForTest() {
		return createBeanResolverForTest( ConfigurationPropertySource.empty() );
	}

	public BeanResolver createBeanResolverForTest(ConfigurationPropertySource configurationPropertySource) {
		AggregatedClassLoader aggregatedClassLoader = AggregatedClassLoader.createDefault();
		ClassResolver classResolver = DefaultClassResolver.create( aggregatedClassLoader );
		ServiceResolver serviceResolver = DefaultServiceResolver.create( aggregatedClassLoader );
		ReflectionBeanProvider beanProvider = ReflectionBeanProvider.create( classResolver );
		return new ConfiguredBeanResolver(
				serviceResolver, beanProvider, configurationPropertySource
		);
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

	public Map<String, Object> interpolateProperties(Map<String, Object> properties) {
		Map<String, Object> interpolatedProperties = new LinkedHashMap<>();

		properties.forEach( (k, v) -> {
			if ( v instanceof String ) {
				interpolatedProperties.put(
						k,
						( (String) v ).replace( "#{test.id}", testId )
								.replace( "#{test.startup.timestamp}", STARTUP_TIMESTAMP ) );
			}
			else {
				interpolatedProperties.put( k, v );
			}
		} );

		return interpolatedProperties;
	}
}
