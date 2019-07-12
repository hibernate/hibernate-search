/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.bridge.binding.BindingContext;

abstract class AbstractBindingContext implements BindingContext {

	private final BeanResolver beanResolver;

	public AbstractBindingContext(BeanResolver beanResolver) {
		this.beanResolver = beanResolver;
	}

	@Override
	public BeanResolver getBeanResolver() {
		return beanResolver;
	}

}
