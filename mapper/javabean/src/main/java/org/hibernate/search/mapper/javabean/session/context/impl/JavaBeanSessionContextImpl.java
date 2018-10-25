/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session.context.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.context.spi.PojoSessionContextImplementor;


public class JavaBeanSessionContextImpl extends PojoSessionContextImplementor {

	private final String tenantId;
	private final PojoRuntimeIntrospector proxyIntrospector;

	public JavaBeanSessionContextImpl(String tenantId, PojoRuntimeIntrospector proxyIntrospector) {
		this.tenantId = tenantId;
		this.proxyIntrospector = proxyIntrospector;
	}

	@Override
	public String getTenantIdentifier() {
		return tenantId;
	}

	@Override
	public PojoRuntimeIntrospector getRuntimeIntrospector() {
		return proxyIntrospector;
	}
}
