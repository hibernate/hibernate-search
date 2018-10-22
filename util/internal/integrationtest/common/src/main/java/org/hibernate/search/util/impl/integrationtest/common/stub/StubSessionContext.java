/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub;

import org.hibernate.search.engine.mapper.session.context.SessionContext;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

public class StubSessionContext implements SessionContextImplementor, SessionContext {

	private final String tenantIdentifier;

	public StubSessionContext() {
		this( null );
	}

	public StubSessionContext(String tenantIdentifier) {
		this.tenantIdentifier = tenantIdentifier;
	}

	@Override
	public <T> T unwrap(Class<T> clazz) {
		return clazz.cast( this );
	}

	@Override
	public SessionContext toAPI() {
		return this;
	}

	@Override
	public String getTenantIdentifier() {
		return tenantIdentifier;
	}
}
