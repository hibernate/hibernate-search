/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.bean.spi.BeanNotFoundException;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ServiceResolver;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class BeanResolverImpl implements BeanResolver {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<List<BeanReference<? extends BeanConfigurer>>> BEAN_CONFIGURERS =
			ConfigurationProperty.forKey( EngineSpiSettings.Radicals.BEAN_CONFIGURERS )
					.asBeanReference( BeanConfigurer.class )
					.multivalued()
					.withDefault( EngineSpiSettings.Defaults.BEAN_CONFIGURERS )
					.build();

	public static BeanResolverImpl create(ClassResolver classResolver, ServiceResolver serviceResolver,
			BeanProvider beanManagerBeanProvider, ConfigurationPropertySource configurationPropertySource) {
		if ( beanManagerBeanProvider == null ) {
			beanManagerBeanProvider = new NoConfiguredBeanManagerBeanProvider();
		}

		ReflectionBeanProvider reflectionBeanProvider = new ReflectionBeanProvider( classResolver );

		BeanConfigurationContextImpl configurationContext = new BeanConfigurationContextImpl();
		for ( BeanConfigurer beanConfigurer : serviceResolver.loadJavaServices( BeanConfigurer.class ) ) {
			beanConfigurer.configure( configurationContext );
		}

		ConfigurationBeanRegistry emptyBeanRegistry = new ConfigurationBeanRegistry( Collections.emptyMap() );
		BeanResolverImpl beanResolverForConfigurers =
				new BeanResolverImpl( emptyBeanRegistry, beanManagerBeanProvider, reflectionBeanProvider );
		try ( BeanHolder<List<BeanConfigurer>> beanConfigurersFromConfigurationProperties =
				BEAN_CONFIGURERS.getAndTransform( configurationPropertySource, beanResolverForConfigurers::resolve ) ) {
			for ( BeanConfigurer beanConfigurer : beanConfigurersFromConfigurationProperties.get() ) {
				beanConfigurer.configure( configurationContext );
			}
		}

		return new BeanResolverImpl( configurationContext.buildRegistry(), beanManagerBeanProvider,
				reflectionBeanProvider );
	}

	private final ConfigurationBeanRegistry configurationBeanRegistry;
	private final BeanProvider beanManagerBeanProvider;
	private final BeanProvider reflectionBeanProvider;

	private BeanResolverImpl(ConfigurationBeanRegistry configurationBeanRegistry,
			BeanProvider beanManagerBeanProvider, ReflectionBeanProvider reflectionBeanProvider) {
		this.configurationBeanRegistry = configurationBeanRegistry;
		this.beanManagerBeanProvider = beanManagerBeanProvider;
		this.reflectionBeanProvider = reflectionBeanProvider;
	}

	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		return resolveFromFirstSuccessfulSource( typeReference, BeanSource.values() );
	}

	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference, String nameReference) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		Contracts.assertNotNullNorEmpty( nameReference, "nameReference" );
		return resolveFromFirstSuccessfulSource( typeReference, nameReference, BeanSource.values() );
	}

	@Override
	public <T> List<BeanReference<T>> allConfiguredForRole(Class<T> role) {
		Contracts.assertNotNull( role, "role" );
		BeanReferenceRegistryForType<T> registry = configurationBeanRegistry.explicitlyConfiguredBeans( role );
		if ( registry == null ) {
			return Collections.emptyList();
		}
		return registry.all();
	}

	@Override
	public <T> Map<String, BeanReference<T>> namedConfiguredForRole(Class<T> role) {
		Contracts.assertNotNull( role, "role" );
		BeanReferenceRegistryForType<T> registry = configurationBeanRegistry.explicitlyConfiguredBeans( role );
		if ( registry == null ) {
			return Collections.emptyMap();
		}
		return registry.named();
	}

	private <T> BeanHolder<T> resolveFromFirstSuccessfulSource(Class<T> typeReference, BeanSource[] sources) {
		BeanNotFoundException firstFailure = null;
		List<BeanNotFoundException> otherFailures = new ArrayList<>();
		for ( BeanSource source : sources ) {
			try {
				return tryResolve( typeReference, source );
			}
			catch (BeanNotFoundException e) {
				if ( firstFailure == null ) {
					firstFailure = e;
				}
				else {
					otherFailures.add( e );
				}
			}
		}
		throw log.cannotResolveBeanReference( typeReference,
				buildFailureMessage( sources, firstFailure, otherFailures ), firstFailure, otherFailures );
	}

	private <T> BeanHolder<T> resolveFromFirstSuccessfulSource(Class<T> typeReference, String nameReference,
			BeanSource[] sources) {
		BeanNotFoundException firstFailure = null;
		List<BeanNotFoundException> otherFailures = new ArrayList<>();
		for ( BeanSource source : sources ) {
			try {
				return tryResolve( typeReference, nameReference, source );
			}
			catch (BeanNotFoundException e) {
				if ( firstFailure == null ) {
					firstFailure = e;
				}
				else {
					otherFailures.add( e );
				}
			}
		}
		throw log.cannotResolveBeanReference( typeReference, nameReference,
				buildFailureMessage( sources, firstFailure, otherFailures ), firstFailure, otherFailures );
	}

	private <T> BeanHolder<T> tryResolve(Class<T> typeReference, BeanSource source) {
		switch ( source ) {
			case CONFIGURATION:
				return configurationBeanRegistry.resolve( typeReference, this );
			case BEAN_MANAGER:
				return beanManagerBeanProvider.forType( typeReference );
			case REFLECTION:
				return reflectionBeanProvider.forType( typeReference );
			default:
				throw unknownBeanSource( source );
		}
	}

	private <T> BeanHolder<T> tryResolve(Class<T> typeReference, String nameReference, BeanSource source) {
		switch ( source ) {
			case CONFIGURATION:
				return configurationBeanRegistry.resolve( typeReference, nameReference, this );
			case BEAN_MANAGER:
				return beanManagerBeanProvider.forTypeAndName( typeReference, nameReference );
			case REFLECTION:
				return reflectionBeanProvider.forTypeAndName( typeReference, nameReference );
			default:
				throw unknownBeanSource( source );
		}
	}

	private String buildFailureMessage(BeanSource[] sources, BeanNotFoundException firstFailure,
			List<BeanNotFoundException> otherFailures) {
		StringBuilder builder = new StringBuilder();
		builder.append( renderFailure( sources[0], firstFailure ) );
		for ( int i = 0; i < otherFailures.size(); i++ ) {
			RuntimeException failure = otherFailures.get( i );
			builder.append( " " );
			builder.append( renderFailure( sources[i + 1], failure ) );
		}
		return builder.toString();
	}

	private String renderFailure(BeanSource source, RuntimeException failure) {
		switch ( source ) {
			case CONFIGURATION:
				return log.failedToResolveBeanUsingInternalRegistry( failure.getMessage() );
			case BEAN_MANAGER:
				return log.failedToResolveBeanUsingBeanManager( failure.getMessage() );
			case REFLECTION:
				return log.failedToResolveBeanUsingReflection( failure.getMessage() );
			default:
				throw unknownBeanSource( source );
		}
	}

	private AssertionFailure unknownBeanSource(BeanSource source) {
		return new AssertionFailure( "Unknown bean source: " + source );
	}

}
