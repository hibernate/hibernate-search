/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.context.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.orm.session.context.HibernateOrmSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;

public class HibernateOrmSessionContextImpl extends AbstractPojoBackendSessionContext implements HibernateOrmSessionContext {

	private final HibernateOrmMappingContextImpl mappingContext;
	private final SessionImplementor sessionImplementor;
	private final PojoRuntimeIntrospector runtimeIntrospector;

	public HibernateOrmSessionContextImpl(HibernateOrmMappingContextImpl mappingContext,
			SessionImplementor sessionImplementor,
			PojoRuntimeIntrospector runtimeIntrospector) {
		this.mappingContext = mappingContext;
		this.sessionImplementor = sessionImplementor;
		this.runtimeIntrospector = runtimeIntrospector;
	}

	@Override
	public HibernateOrmMappingContextImpl getMappingContext() {
		return mappingContext;
	}

	@Override
	public String getTenantIdentifier() {
		return sessionImplementor.getTenantIdentifier();
	}

	@Override
	public PojoRuntimeIntrospector getRuntimeIntrospector() {
		return runtimeIntrospector;
	}

	@Override
	public SessionImplementor getSession() {
		return sessionImplementor;
	}
}
