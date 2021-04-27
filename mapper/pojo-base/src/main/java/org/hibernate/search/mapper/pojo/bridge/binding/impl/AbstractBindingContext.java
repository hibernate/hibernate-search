/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.bridge.binding.BindingContext;

abstract class AbstractBindingContext implements BindingContext {

	private final BeanResolver beanResolver;
	private final Map<String, Object> params;

	AbstractBindingContext(BeanResolver beanResolver, Map<String, Object> params) {
		this.beanResolver = beanResolver;
		this.params = params;
	}

	@Override
	public BeanResolver beanResolver() {
		return beanResolver;
	}

	@Override
	public Object param(String name) {
		return params.get( name );
	}
}
