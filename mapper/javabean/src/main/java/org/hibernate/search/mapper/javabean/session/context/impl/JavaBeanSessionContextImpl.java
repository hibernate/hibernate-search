/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session.context.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.mapper.session.context.SessionContext;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.javabean.session.context.JavaBeanSessionContext;
import org.hibernate.search.mapper.pojo.session.context.spi.PojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.impl.common.LoggerFactory;


public class JavaBeanSessionContextImpl extends PojoSessionContextImplementor implements JavaBeanSessionContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String tenantId;
	private final PojoRuntimeIntrospector proxyIntrospector;

	public JavaBeanSessionContextImpl(String tenantId, PojoRuntimeIntrospector proxyIntrospector) {
		this.tenantId = tenantId;
		this.proxyIntrospector = proxyIntrospector;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( JavaBeanSessionContext.class ) ) {
			return (T) this;
		}
		throw log.sessionContextUnwrappingWithUnknownType( clazz, JavaBeanSessionContext.class );
	}

	@Override
	public SessionContext toAPI() {
		return this;
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
