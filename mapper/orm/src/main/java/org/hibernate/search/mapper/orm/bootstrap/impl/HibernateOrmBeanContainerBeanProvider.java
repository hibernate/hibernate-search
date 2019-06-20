/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.bean.spi.ReflectionBeanProvider;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * A {@link BeanProvider} relying on a Hibernate ORM {@link BeanContainer} to resolve beans.
 */
final class HibernateOrmBeanContainerBeanProvider implements BeanProvider {

	private static final BeanContainer.LifecycleOptions LIFECYCLE_OPTIONS = new BeanContainer.LifecycleOptions() {
		@Override
		public boolean canUseCachedReferences() {
			return false;
		}

		@Override
		public boolean useJpaCompliantCreation() {
			return false;
		}
	};

	private final BeanContainer beanContainer;

	private final ReflectionBeanProvider fallback;
	private final BeanInstanceProducer fallbackInstanceProducer;

	HibernateOrmBeanContainerBeanProvider(BeanContainer beanContainer, ReflectionBeanProvider fallback) {
		Contracts.assertNotNull( beanContainer, "beanContainer" );
		this.beanContainer = beanContainer;
		this.fallback = fallback;
		this.fallbackInstanceProducer = new BeanInstanceProducer() {
			private final ReflectionBeanProvider delegate = fallback;

			@Override
			public <B> B produceBeanInstance(Class<B> aClass) {
				return delegate.getBeanNoClosingNecessary( aClass );
			}

			@Override
			public <B> B produceBeanInstance(String s, Class<B> aClass) {
				return delegate.getBeanNoClosingNecessary( aClass, s );
			}
		};
	}

	@Override
	public void close() {
		fallback.close();
	}

	@Override
	public <T> BeanHolder<T> getBean(Class<T> typeReference) {
		ContainedBean<T> containedBean = beanContainer.getBean(
				typeReference, LIFECYCLE_OPTIONS, fallbackInstanceProducer
		);
		return new HibernateOrmContainedBeanBeanHolderAdapter<>( containedBean );
	}

	@Override
	public <T> BeanHolder<T> getBean(Class<T> typeReference, String nameReference) {
		ContainedBean<T> containedBean = beanContainer.getBean(
				nameReference, typeReference, LIFECYCLE_OPTIONS, fallbackInstanceProducer
		);
		return new HibernateOrmContainedBeanBeanHolderAdapter<>( containedBean );
	}

}
