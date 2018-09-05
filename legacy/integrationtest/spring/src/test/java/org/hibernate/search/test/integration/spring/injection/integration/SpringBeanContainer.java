/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.injection.integration;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.springframework.beans.factory.BeanFactory;


/**
 * @author Yoann Rodiere
 */
public class SpringBeanContainer implements BeanContainer {

	private final BeanFactory beanFactory;

	public SpringBeanContainer(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public <B> ContainedBean<B> getBean(Class<B> beanClass, LifecycleOptions lifecycleOptions,
			BeanInstanceProducer beanInstanceProducer) {
		checkLifecycleOptions( lifecycleOptions );

		if ( beanClass.isAnnotationPresent( ResolveInHibernate.class ) ) {
			return new SpringManagedBean<>( beanFactory.getBean( beanClass ) );
		}
		else {
			return new DefaultBeanInstanceProducerBean<>(
					beanInstanceProducer.produceBeanInstance( beanClass )
			);
		}
	}

	@Override
	public <B> ContainedBean<B> getBean(String beanName, Class<B> beanContract,
			LifecycleOptions lifecycleOptions, BeanInstanceProducer beanInstanceProducer) {
		if ( beanContract.isAnnotationPresent( ResolveInHibernate.class ) ) {
			return new SpringManagedBean<>( beanFactory.getBean( beanName, beanContract ) );
		}
		else {
			return new DefaultBeanInstanceProducerBean<>(
					beanInstanceProducer.produceBeanInstance( beanName, beanContract )
			);
		}
	}

	@Override
	public void stop() {
		// Nothing to do
	}

	private void checkLifecycleOptions(LifecycleOptions lifecycleOptions) {
		if ( lifecycleOptions.canUseCachedReferences() || lifecycleOptions.useJpaCompliantCreation() ) {
			throw new UnsupportedOperationException(
					"This bean container does not support JPA-compliant creation nor caching references."
							+ " If you see this exception, something went wrong, since those two features "
							+ " are not supposed to be used in these tests."
			);
		}
	}

	private static class SpringManagedBean<T> implements ContainedBean<T> {

		private final T beanInstance;

		SpringManagedBean(T beanInstance) {
			this.beanInstance = beanInstance;
		}

		@Override
		public T getBeanInstance() {
			return beanInstance;
		}
	}

	private class DefaultBeanInstanceProducerBean<T> implements ContainedBean<T> {

		private final T beanInstance;

		DefaultBeanInstanceProducerBean(T beanInstance) {
			this.beanInstance = beanInstance;
		}

		@Override
		public T getBeanInstance() {
			return beanInstance;
		}
	}
}
