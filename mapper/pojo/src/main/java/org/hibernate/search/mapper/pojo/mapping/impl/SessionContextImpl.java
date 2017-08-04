/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import org.hibernate.search.engine.common.spi.SessionContext;


/**
 * @author Yoann Rodiere
 */
class SessionContextImpl implements SessionContext {
	private final String tenantId;

	public SessionContextImpl(String tenantId) {
		super();
		this.tenantId = tenantId;
	}

	@Override
	public String getTenantIdentifier() {
		return tenantId;
	}

}
