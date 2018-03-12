/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;


/**
 * @author Yoann Rodiere
 */
public class PojoSessionContextImpl implements PojoSessionContext {
	private final PojoRuntimeIntrospector proxyIntrospector;
	private final String tenantId;

	public PojoSessionContextImpl(PojoRuntimeIntrospector proxyIntrospector, String tenantId) {
		this.proxyIntrospector = proxyIntrospector;
		this.tenantId = tenantId;
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
