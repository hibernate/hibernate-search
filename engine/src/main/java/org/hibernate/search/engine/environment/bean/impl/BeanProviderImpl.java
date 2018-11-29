/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.bean.spi.BeanCreationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanFactory;
import org.hibernate.search.engine.environment.bean.spi.BeanResolver;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.StringHelper;

public final class BeanProviderImpl implements BeanProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanResolver beanResolver;
	private final Map<ConfiguredBeanKey<?>, BeanFactory<?>> explicitlyConfiguredBeans;

	private final BeanCreationContext beanCreationContext;

	public BeanProviderImpl(ClassResolver classResolver, BeanResolver beanResolver) {
		this.beanResolver = beanResolver;

		// TODO maybe also add a way to pass configurer through a configuration property?
		BeanConfigurationContextImpl configurationContext = new BeanConfigurationContextImpl();
		for ( BeanConfigurer beanConfigurer : classResolver.loadJavaServices( BeanConfigurer.class ) ) {
			beanConfigurer.configure( configurationContext );
		}
		this.explicitlyConfiguredBeans = configurationContext.getConfiguredBeans();

		this.beanCreationContext = new BeanCreationContextImpl( this );
	}

	@Override
	public <T> T getBean(Class<T> expectedClass, Class<?> typeReference) {
		if ( typeReference == null ) {
			throw log.invalidBeanReferenceTypeNull();
		}
		return beanResolver.resolve( expectedClass, typeReference );
	}

	@Override
	public <T> T getBean(Class<T> expectedClass, String nameReference) {
		if ( StringHelper.isEmpty( nameReference ) ) {
			throw log.invalidBeanReferenceNameNullOrEmpty();
		}
		return getBeanFromBeanResolverOrConfiguredBeans( expectedClass, nameReference );
	}

	@Override
	public <T> T getBean(Class<T> expectedClass, Class<?> typeReference, String nameReference) {
		if ( typeReference == null ) {
			throw log.invalidBeanReferenceTypeNull();
		}
		if ( StringHelper.isEmpty( nameReference ) ) {
			throw log.invalidBeanReferenceNameNullOrEmpty();
		}
		return beanResolver.resolve( expectedClass, nameReference, typeReference );
	}

	private <T> T getBeanFromBeanResolverOrConfiguredBeans(Class<T> expectedClass, String nameReference) {
		try {
			return beanResolver.resolve( expectedClass, nameReference );
		}
		catch (SearchException e) {
			/*
			 * Fall back to an explicitly configured bean.
			 * It's important to do this *after* trying the bean resolver,
			 * so that adding explicitly configured beans in a new version of Hibernate Search
			 * doesn't break existing user's configuration.
			 */
			try {
				T explicitlyConfiguredBean = getExplicitlyConfiguredBean( expectedClass, nameReference );
				if ( explicitlyConfiguredBean != null ) {
					return explicitlyConfiguredBean;
				}
			}
			catch (RuntimeException e2) {
				e.addSuppressed( e2 );
			}
			throw e;
		}
	}

	private <T> T getExplicitlyConfiguredBean(Class<T> exposedType, String name) {
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
