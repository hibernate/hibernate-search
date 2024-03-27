/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.bean.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.common.spi.SearchIntegrationEnvironment;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.bean.spi.BeanNotFoundException;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.classpath.spi.ClassLoaderHelper;
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
			BeanProvider beanManagerBeanProvider, ConfigurationPropertySource rawConfigurationPropertySource) {
		if ( beanManagerBeanProvider == null ) {
			beanManagerBeanProvider = new NoConfiguredBeanManagerBeanProvider();
		}

		BeanConfigurationContextImpl configurationContext = new BeanConfigurationContextImpl();
		for ( BeanConfigurer beanConfigurer : serviceResolver.loadJavaServices( BeanConfigurer.class ) ) {
			beanConfigurer.configure( configurationContext );
		}

		// The bean resolver used to resolve the ConfigurationProvider
		// and the BeanConfigurers set through configuration properties
		// will only take into account BeanConfigurers registered through the Java ServiceLoader,
		// not those set through configuration properties.
		ConfigurationBeanRegistry beanRegistryForConfiguration = configurationContext.buildRegistry();
		BeanResolverImpl beanResolverForConfiguration =
				new BeanResolverImpl( classResolver, beanRegistryForConfiguration, beanManagerBeanProvider );
		ConfigurationPropertySource configurationPropertySource = SearchIntegrationEnvironment.rootPropertySource(
				rawConfigurationPropertySource, beanResolverForConfiguration );
		try ( BeanHolder<List<BeanConfigurer>> beanConfigurersFromConfigurationProperties =
				BEAN_CONFIGURERS.getAndTransform( configurationPropertySource, beanResolverForConfiguration::resolve ) ) {
			for ( BeanConfigurer beanConfigurer : beanConfigurersFromConfigurationProperties.get() ) {
				beanConfigurer.configure( configurationContext );
			}
		}

		return new BeanResolverImpl( classResolver, configurationContext.buildRegistry(), beanManagerBeanProvider );
	}

	private final ClassResolver classResolver;
	private final ConfigurationBeanRegistry configurationBeanRegistry;
	private final BeanProvider beanManagerBeanProvider;

	private BeanResolverImpl(ClassResolver classResolver,
			ConfigurationBeanRegistry configurationBeanRegistry,
			BeanProvider beanManagerBeanProvider) {
		this.classResolver = classResolver;
		this.configurationBeanRegistry = configurationBeanRegistry;
		this.beanManagerBeanProvider = beanManagerBeanProvider;
	}

	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference, BeanRetrieval retrieval) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		return resolveFromFirstSuccessfulSource( typeReference, retrieval );
	}

	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference, String nameReference, BeanRetrieval retrieval) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		Contracts.assertNotNullNorEmpty( nameReference, "nameReference" );
		return resolveFromFirstSuccessfulSource( typeReference, nameReference, retrieval );
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

	private List<BeanSource> toSources(BeanRetrieval retrieval, boolean hasName) {
		switch ( retrieval ) {
			case BUILTIN:
				return Collections.singletonList( BeanSource.CONFIGURATION );
			case BEAN:
				return Collections.singletonList( BeanSource.BEAN_MANAGER );
			case CLASS:
				return Arrays.asList( BeanSource.BEAN_MANAGER_ASSUME_CLASS_NAME, BeanSource.REFLECTION );
			case CONSTRUCTOR:
				return Collections.singletonList( BeanSource.REFLECTION );
			case ANY:
				return hasName
						? Arrays.asList( BeanSource.CONFIGURATION, BeanSource.BEAN_MANAGER,
								BeanSource.BEAN_MANAGER_ASSUME_CLASS_NAME, BeanSource.REFLECTION )
						: Arrays.asList( BeanSource.CONFIGURATION, BeanSource.BEAN_MANAGER, BeanSource.REFLECTION );
			default:
				throw new AssertionFailure( "Unknown bean retrieval: " + retrieval );
		}
	}

	private <T> BeanHolder<T> resolveFromFirstSuccessfulSource(Class<T> typeReference, BeanRetrieval retrieval) {
		List<BeanSource> sources = toSources( retrieval, false );
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
			BeanRetrieval retrieval) {
		List<BeanSource> sources = toSources( retrieval, true );
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
			case BEAN_MANAGER_ASSUME_CLASS_NAME:
				return beanManagerBeanProvider.forType( typeReference );
			case REFLECTION:
				return retrieveUsingConstructor( typeReference );
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
			case BEAN_MANAGER_ASSUME_CLASS_NAME:
				return beanManagerBeanProvider.forType( toClass( typeReference, nameReference ) );
			case REFLECTION:
				return retrieveUsingConstructor( toClass( typeReference, nameReference ) );
			default:
				throw unknownBeanSource( source );
		}
	}

	public <T> Class<? extends T> toClass(Class<T> typeReference, String nameReference) {
		try {
			return ClassLoaderHelper.classForName( typeReference, nameReference, classResolver );
		}
		catch (RuntimeException e) {
			throw log.unableToResolveToClassName( typeReference, nameReference, e.getMessage(), e );
		}
	}

	public <T> BeanHolder<T> retrieveUsingConstructor(Class<T> typeReference) {
		try {
			return BeanHolder.of( ClassLoaderHelper.untypedInstanceFromClass( typeReference ) );
		}
		catch (RuntimeException e) {
			throw log.unableToCreateBeanUsingReflection( e.getMessage(), e );
		}
	}

	private String buildFailureMessage(List<BeanSource> sources, BeanNotFoundException firstFailure,
			List<BeanNotFoundException> otherFailures) {
		StringBuilder builder = new StringBuilder();
		builder.append( renderFailure( sources.get( 0 ), firstFailure ) );
		for ( int i = 0; i < otherFailures.size(); i++ ) {
			RuntimeException failure = otherFailures.get( i );
			builder.append( " " );
			builder.append( renderFailure( sources.get( i + 1 ), failure ) );
		}
		return builder.toString();
	}

	private String renderFailure(BeanSource source, RuntimeException failure) {
		switch ( source ) {
			case CONFIGURATION:
				return log.failedToResolveBeanUsingInternalRegistry( failure.getMessage() );
			case BEAN_MANAGER:
				return log.failedToResolveBeanUsingBeanManager( failure.getMessage() );
			case BEAN_MANAGER_ASSUME_CLASS_NAME:
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
