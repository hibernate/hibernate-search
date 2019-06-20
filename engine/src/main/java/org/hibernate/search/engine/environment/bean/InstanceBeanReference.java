/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

import org.hibernate.search.util.common.impl.Contracts;

final class InstanceBeanReference<T> implements BeanReference<T> {

	private final T instance;

	InstanceBeanReference(T instance) {
		Contracts.assertNotNull( instance, "instance" );
		this.instance = instance;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[instance=" + instance + "]";
	}

	@Override
	public BeanHolder<T> resolve(BeanResolver beanResolver) {
		return BeanHolder.of( instance );
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <U> BeanReference<? extends U> asSubTypeOf(Class<U> expectedType) {
		// Let the type itself throw a ClassCastException if something is wrong
		expectedType.cast( instance );
		// The cast above worked, so we can do this safely:
		return (BeanReference<? extends U>) this;
	}
}
