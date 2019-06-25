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
import java.util.regex.Pattern;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.bean.spi.BeanCreationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanFactory;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Contracts;

public final class ConfiguredBeanResolver implements BeanResolver {

	private static final ConfigurationProperty<List<BeanReference<? extends BeanConfigurer>>> BEAN_CONFIGURERS =
			ConfigurationProperty.forKey( EngineSpiSettings.BEAN_CONFIGURERS )
					.asBeanReference( BeanConfigurer.class )
					.multivalued( Pattern.compile( "\\s+" ) )
					.withDefault( EngineSpiSettings.Defaults.BEAN_CONFIGURERS )
					.build();

	private final BeanProvider beanProvider;
	private final Map<ConfiguredBeanKey<?>, BeanFactory<?>> explicitlyConfiguredBeans;
	private final Map<Class<?>, List<? extends BeanReference<?>>> roleMap;

	private final BeanCreationContext beanCreationContext;

	public ConfiguredBeanResolver(ClassResolver classResolver, BeanProvider beanProvider,
			ConfigurationPropertySource configurationPropertySource) {
		this.beanProvider = beanProvider;

		BeanConfigurationContextImpl configurationContext = new BeanConfigurationContextImpl();
		for ( BeanConfigurer beanConfigurer : classResolver.loadJavaServices( BeanConfigurer.class ) ) {
			beanConfigurer.configure( configurationContext );
		}
		BeanProviderOnlyBeanResolver beanResolverForConfigurers = new BeanProviderOnlyBeanResolver( beanProvider );
		try ( BeanHolder<List<BeanConfigurer>> beanConfigurersFromConfigurationProperties =
				BEAN_CONFIGURERS.getAndTransform( configurationPropertySource, beanResolverForConfigurers::resolve ) ) {
			for ( BeanConfigurer beanConfigurer : beanConfigurersFromConfigurationProperties.get() ) {
				beanConfigurer.configure( configurationContext );
			}
		}
		this.explicitlyConfiguredBeans = configurationContext.getConfiguredBeans();
		this.roleMap = configurationContext.getRoleMap();

		this.beanCreationContext = new BeanCreationContextImpl( this );
	}

	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		try {
			return beanProvider.getBean( typeReference );
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
			return beanProvider.getBean( typeReference, nameReference );
		}
		catch (SearchException e) {
			return fallbackToConfiguredBeans( e, typeReference, nameReference );
		}
	}

	@Override
	public <T> BeanHolder<List<T>> resolveRole(Class<T> role) {
		Contracts.assertNotNull( role, "role" );
		@SuppressWarnings("unchecked") // We know the references have the correct type, see BeanConfigurationContextImpl
		List<BeanReference<? extends T>> references = (List<BeanReference<? extends T>>) roleMap.get( role );
		if ( references == null || references.isEmpty() ) {
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
	private <T> BeanHolder<T> fallbackToConfiguredBeans(SearchException e, Class<T> typeReference, String nameReference) {
		try {
			BeanHolder<T> explicitlyConfiguredBean = getExplicitlyConfiguredBean( typeReference, nameReference );
			if ( explicitlyConfiguredBean != null ) {
				return explicitlyConfiguredBean;
			}
		}
		catch (RuntimeException e2) {
			e.addSuppressed( e2 );
		}
		throw e;
	}

	private <T> BeanHolder<T> getExplicitlyConfiguredBean(Class<T> exposedType, String name) {
		ConfiguredBeanKey<T> key = new ConfiguredBeanKey<>( exposedType, name );
		@SuppressWarnings("unchecked") // We know the factory has the correct type, see BeanConfigurationContextImpl
		BeanFactory<T> factory = (BeanFactory<T>) explicitlyConfiguredBeans.get( key );
		if ( factory == null ) {
			return null;
		}
		else {
			return factory.create( beanCreationContext );
		}
	}

}
