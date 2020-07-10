/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

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
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.classpath.spi.ServiceResolver;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Contracts;

public final class ConfiguredBeanResolver implements BeanResolver {

	private static final ConfigurationProperty<List<BeanReference<? extends BeanConfigurer>>> BEAN_CONFIGURERS =
			ConfigurationProperty.forKey( EngineSpiSettings.Radicals.BEAN_CONFIGURERS )
					.asBeanReference( BeanConfigurer.class )
					.multivalued()
					.withDefault( EngineSpiSettings.Defaults.BEAN_CONFIGURERS )
					.build();

	private final BeanProvider beanProvider;
	private final Map<Class<?>, BeanReferenceRegistryForType<?>> explicitlyConfiguredBeans;

	public ConfiguredBeanResolver(ServiceResolver serviceResolver, BeanProvider beanProvider,
			ConfigurationPropertySource configurationPropertySource) {
		this.beanProvider = beanProvider;

		BeanConfigurationContextImpl configurationContext = new BeanConfigurationContextImpl();
		for ( BeanConfigurer beanConfigurer : serviceResolver.loadJavaServices( BeanConfigurer.class ) ) {
			beanConfigurer.configure( configurationContext );
		}
		BeanProviderOnlyBeanResolver beanResolverForConfigurers = new BeanProviderOnlyBeanResolver( beanProvider );
		try ( BeanHolder<List<BeanConfigurer>> beanConfigurersFromConfigurationProperties =
				BEAN_CONFIGURERS.getAndTransform( configurationPropertySource, beanResolverForConfigurers::resolve ) ) {
			for ( BeanConfigurer beanConfigurer : beanConfigurersFromConfigurationProperties.get() ) {
				beanConfigurer.configure( configurationContext );
			}
		}
		this.explicitlyConfiguredBeans = configurationContext.configuredBeans();
	}

	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		try {
			return beanProvider.forType( typeReference );
		}
		catch (SearchException e) {
			return fallbackToConfiguredBeans( e, typeReference, null );
		}
	}

	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference, String nameReference) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		Contracts.assertNotNullNorEmpty( nameReference, "nameReference" );
		try {
			return beanProvider.forTypeAndName( typeReference, nameReference );
		}
		catch (SearchException e) {
			return fallbackToConfiguredBeans( e, typeReference, nameReference );
		}
	}

	@Override
	public <T> BeanHolder<List<T>> resolveRole(Class<T> role) {
		Contracts.assertNotNull( role, "role" );
		BeanReferenceRegistryForType<T> registry = explicitlyConfiguredBeans( role );
		if ( registry == null ) {
			return BeanHolder.of( Collections.emptyList() );
		}
		List<BeanReference<? extends T>> references = registry.all();
		if ( references.isEmpty() ) {
			return BeanHolder.of( Collections.emptyList() );
		}
		else {
			return resolve( references );
		}
	}

	/*
	 * Fall back to an explicitly configured bean.
	 * It's important to do this *after* trying the bean resolver,
	 * so that adding explicitly configured beans in a new version of Hibernate Search
	 * doesn't break existing user's configuration.
	 */
	private <T> BeanHolder<T> fallbackToConfiguredBeans(SearchException e, Class<T> exposedType, String nameOrNull) {
		BeanReference<T> reference = null;
		try {
			BeanReferenceRegistryForType<T> registry = explicitlyConfiguredBeans( exposedType );
			if ( registry != null ) {
				reference = registry.single( nameOrNull );
			}
		}
		catch (RuntimeException e2) {
			e.addSuppressed( e2 );
		}
		if ( reference != null ) {
			return reference.resolve( this );
		}
		else {
			throw e;
		}
	}

	@SuppressWarnings("unchecked") // We know the registry has the correct type, see BeanConfigurationContextImpl
	private <T> BeanReferenceRegistryForType<T> explicitlyConfiguredBeans(Class<T> exposedType) {
		return (BeanReferenceRegistryForType<T>) explicitlyConfiguredBeans.get( exposedType );
	}

}
