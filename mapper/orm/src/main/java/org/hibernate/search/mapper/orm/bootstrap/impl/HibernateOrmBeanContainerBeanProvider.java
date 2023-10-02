/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A {@link BeanProvider} relying on a Hibernate ORM {@link BeanContainer} to resolve beans.
 */
final class HibernateOrmBeanContainerBeanProvider implements BeanProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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

	private final BeanInstanceProducer fallbackInstanceProducer;

	HibernateOrmBeanContainerBeanProvider(BeanContainer beanContainer) {
		Contracts.assertNotNull( beanContainer, "beanContainer" );
		this.beanContainer = beanContainer;
		this.fallbackInstanceProducer = new BeanInstanceProducer() {
			@Override
			public <B> B produceBeanInstance(Class<B> aClass) {
				throw log.beanNotFoundInBeanContainer( beanContainer );
			}

			@Override
			public <B> B produceBeanInstance(String s, Class<B> aClass) {
				throw log.beanNotFoundInBeanContainer( beanContainer );
			}
		};
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public <T> BeanHolder<T> forType(Class<T> typeReference) {
		ContainedBean<T> containedBean = beanContainer.getBean(
				typeReference, LIFECYCLE_OPTIONS, fallbackInstanceProducer
		);
		BeanHolder<T> result = new HibernateOrmContainedBeanBeanHolderAdapter<>( containedBean );
		// In some cases (ExtendedBeanManager in particular), the bean is retrieved lazily.
		// This means the fallback instance producer is never called, which is a problem.
		// Since we don't need lazy retrieval in our case (all beans are retrieved at bootstrap),
		// we trigger initialization ourselves and use the fallback if necessary.
		try {
			result.get();
		}
		catch (Exception e) {
			new SuppressingCloser( e ).push( result );
			log.debugf( e, "Error resolving bean of type [%s] - using fallback", typeReference );
			try {
				result = BeanHolder.of( fallbackInstanceProducer.produceBeanInstance( typeReference ) );
			}
			catch (Exception e2) {
				// Keep track of the original failure to retrieve the bean from the bean container.
				e2.addSuppressed( e );
				throw e2;
			}
		}
		return result;
	}

	@Override
	public <T> BeanHolder<T> forTypeAndName(Class<T> typeReference, String nameReference) {
		ContainedBean<T> containedBean = beanContainer.getBean(
				nameReference, typeReference, LIFECYCLE_OPTIONS, fallbackInstanceProducer
		);
		BeanHolder<T> result = new HibernateOrmContainedBeanBeanHolderAdapter<>( containedBean );
		// In some cases (ExtendedBeanManager in particular), the bean is retrieved lazily.
		// This means the fallback instance producer is never called, which is a problem.
		// Since we don't need lazy retrieval in our case (all beans are retrieved at bootstrap),
		// we trigger initialization ourselves and use the fallback if necessary.
		result.get();
		return result;
	}

}
