/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.ReflectionBeanResolver;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.Closer;

/**
 * A {@link BeanResolver} relying on a Hibernate ORM {@link BeanContainer} to resolve beans.
 */
final class HibernateOrmBeanContainerBeanResolver implements BeanResolver {

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

	private final ConcurrentHashMap<ContainedBeanImplementor, Object> beansToCleanup = new ConcurrentHashMap<>();

	private final BeanInstanceProducer fallbackInstanceProducer = new BeanInstanceProducer() {
		private final BeanResolver delegate = new ReflectionBeanResolver();

		@Override
		public <B> B produceBeanInstance(Class<B> aClass) {
			return delegate.resolve( aClass, aClass );
		}

		@Override
		public <B> B produceBeanInstance(String s, Class<B> aClass) {
			return delegate.resolve( s, aClass );
		}
	};

	HibernateOrmBeanContainerBeanResolver(BeanContainer beanContainer) {
		Objects.requireNonNull( beanContainer );
		this.beanContainer = beanContainer;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( ContainedBeanImplementor::release, beansToCleanup.keySet() );
		}
	}

	@Override
	public <T> T resolve(Class<?> typeReference, Class<T> expectedClass) {
		ContainedBean<?> containedBean = beanContainer.getBean( typeReference, LIFECYCLE_OPTIONS, fallbackInstanceProducer );
		register( containedBean );
		return expectedClass.cast( containedBean.getBeanInstance() );
	}

	@Override
	public <T> T resolve(String nameReference, Class<T> expectedClass) {
		ContainedBean<T> containedBean = beanContainer.getBean( nameReference, expectedClass, LIFECYCLE_OPTIONS, fallbackInstanceProducer );
		register( containedBean );
		return containedBean.getBeanInstance();
	}

	@Override
	public <T> T resolve(String nameReference, Class<?> typeReference, Class<T> expectedClass) {
		throw new SearchException( "This bean resolver does not support"
				+ " bean references using both a name and a type."
				+ " Got both '" + nameReference + "' and '" + typeReference + "' in the same reference" );
	}

	private void register(ContainedBean<?> containedBean) {
		if ( containedBean instanceof ContainedBeanImplementor ) {
			beansToCleanup.put( (ContainedBeanImplementor) containedBean, containedBean );
		}
	}

}
