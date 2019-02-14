/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.bean.spi.BeanCreationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanFactory;
import org.hibernate.search.engine.environment.bean.spi.BeanResolver;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.Contracts;

public final class ConfiguredBeanProvider implements BeanProvider {

	private static final ConfigurationProperty<List<BeanReference<? extends BeanConfigurer>>> BEAN_CONFIGURERS =
			ConfigurationProperty.forKey( EngineSpiSettings.BEAN_CONFIGURERS )
					.asBeanReference( BeanConfigurer.class )
					.multivalued( Pattern.compile( "\\s+" ) )
					.withDefault( EngineSpiSettings.Defaults.BEAN_CONFIGURERS )
					.build();

	private final BeanResolver beanResolver;
	private final Map<ConfiguredBeanKey<?>, BeanFactory<?>> explicitlyConfiguredBeans;

	private final BeanCreationContext beanCreationContext;

	public ConfiguredBeanProvider(ClassResolver classResolver, BeanResolver beanResolver,
			ConfigurationPropertySource configurationPropertySource) {
		this.beanResolver = beanResolver;

		BeanConfigurationContextImpl configurationContext = new BeanConfigurationContextImpl();
		for ( BeanConfigurer beanConfigurer : classResolver.loadJavaServices( BeanConfigurer.class ) ) {
			beanConfigurer.configure( configurationContext );
		}
		BeanResolverOnlyBeanProvider beanProviderForConfigurers = new BeanResolverOnlyBeanProvider( beanResolver );
		try ( BeanHolder<List<BeanConfigurer>> beanConfigurersFromConfigurationProperties =
				BEAN_CONFIGURERS.getAndTransform( configurationPropertySource, beanProviderForConfigurers::getBeans ) ) {
			for ( BeanConfigurer beanConfigurer : beanConfigurersFromConfigurationProperties.get() ) {
				beanConfigurer.configure( configurationContext );
			}
		}
		this.explicitlyConfiguredBeans = configurationContext.getConfiguredBeans();

		this.beanCreationContext = new BeanCreationContextImpl( this );
	}

	@Override
	public <T> BeanHolder<T> getBean(Class<T> typeReference) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		try {
			return beanResolver.resolve( typeReference );
		}
		catch (SearchException e) {
			return fallbackToConfiguredBeans( e, typeReference, null );
		}
	}

	@Override
	public <T> BeanHolder<T> getBean(Class<T> typeReference, String nameReference) {
		Contracts.assertNotNull( typeReference, "typeReference" );
		Contracts.assertNotNullNorEmpty( nameReference, "nameReference" );
		try {
			return beanResolver.resolve( typeReference, nameReference );
		}
		catch (SearchException e) {
			return fallbackToConfiguredBeans( e, typeReference, nameReference );
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
