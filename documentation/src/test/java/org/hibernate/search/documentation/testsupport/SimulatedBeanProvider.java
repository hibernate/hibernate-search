/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.util.common.SearchException;

public final class SimulatedBeanProvider implements BeanProvider {

	public static Builder builder() {
		return new Builder();
	}

	private final Map<Class<?>, BeanHolder<?>> beans;

	private SimulatedBeanProvider(Builder builder) {
		this.beans = builder.beans;
	}

	@Override
	public void close() {
		// No-op
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> BeanHolder<T> forType(Class<T> typeReference) {
		BeanHolder<T> beanHolder = (BeanHolder<T>) beans.get( typeReference );
		if ( beanHolder == null ) {
			throw new SearchException( "Named beans are not supported" );
		}
		return beanHolder;
	}

	@Override
	public <T> BeanHolder<T> forTypeAndName(Class<T> typeReference, String nameReference) {
		throw new SearchException( "Named beans are not supported" );
	}

	public static class Builder {
		private final Map<Class<?>, BeanHolder<?>> beans = new HashMap<>();

		private Builder() {
		}

		public <T> Builder add(Class<T> clazz, T bean) {
			return add( clazz, BeanHolder.of( bean ) );
		}

		public <T> Builder add(Class<T> clazz, BeanHolder<T> beanHolder) {
			beans.put( clazz, beanHolder );
			return this;
		}

		public BeanProvider build() {
			return new SimulatedBeanProvider( this );
		}
	}
}
