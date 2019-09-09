/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session.context.impl;

import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanBackendMappingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;


public class JavaBeanBackendSessionContext extends AbstractPojoBackendSessionContext {

	private final JavaBeanBackendMappingContext mappingContext;
	private final String tenantId;
	private final PojoRuntimeIntrospector proxyIntrospector;

	public JavaBeanBackendSessionContext(JavaBeanBackendMappingContext mappingContext,
			String tenantId, PojoRuntimeIntrospector proxyIntrospector) {
		this.mappingContext = mappingContext;
		this.tenantId = tenantId;
		this.proxyIntrospector = proxyIntrospector;
	}

	@Override
	public JavaBeanBackendMappingContext getMappingContext() {
		return mappingContext;
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
