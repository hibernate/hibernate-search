/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

import org.hibernate.search.util.impl.common.Contracts;

final class InstanceBeanReference implements BeanReference {

	private final Object instance;

	InstanceBeanReference(Object instance) {
		Contracts.assertNotNull( instance, "instance" );
		this.instance = instance;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[instance=" + instance + "]";
	}

	@Override
	public <T> T getBean(BeanProvider beanProvider, Class<T> expectedType) {
		return expectedType.cast( instance );
	}

}
