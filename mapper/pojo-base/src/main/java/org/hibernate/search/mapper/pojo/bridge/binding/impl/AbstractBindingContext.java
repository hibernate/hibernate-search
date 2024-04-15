/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.common.spi.MapNamedValues;
import org.hibernate.search.mapper.pojo.bridge.binding.BindingContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractBindingContext implements BindingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanResolver beanResolver;
	private final NamedValues params;

	AbstractBindingContext(BeanResolver beanResolver, Map<String, Object> params) {
		this.beanResolver = beanResolver;
		this.params = MapNamedValues.fromMap( params, log::paramNotDefined );
	}

	@Override
	public BeanResolver beanResolver() {
		return beanResolver;
	}

	@Override
	public NamedValues params() {
		return params;
	}
}
