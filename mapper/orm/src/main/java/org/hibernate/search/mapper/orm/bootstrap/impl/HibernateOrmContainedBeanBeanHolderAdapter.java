/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.search.engine.environment.bean.BeanHolder;

final class HibernateOrmContainedBeanBeanHolderAdapter<T> implements BeanHolder<T> {

	private final ContainedBean<T> containedBean;

	HibernateOrmContainedBeanBeanHolderAdapter(ContainedBean<T> containedBean) {
		this.containedBean = containedBean;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "containedBean=" + containedBean
				+ "]";
	}

	@Override
	public T get() {
		return containedBean.getBeanInstance();
	}

	@Override
	public void close() {
		if ( containedBean instanceof ContainedBeanImplementor ) {
			( (ContainedBeanImplementor<?>) containedBean ).release();
		}
	}
}
