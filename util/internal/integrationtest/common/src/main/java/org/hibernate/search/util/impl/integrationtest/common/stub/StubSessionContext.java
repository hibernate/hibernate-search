/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub;

import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

public class StubSessionContext implements SessionContextImplementor {

	private final StubMappingContext mappingContext;
	private final String tenantIdentifier;

	public StubSessionContext() {
		this( new StubMappingContext(), null );
	}

	public StubSessionContext(String tenantIdentifier) {
		this( new StubMappingContext(), tenantIdentifier );
	}

	public StubSessionContext(StubMappingContext mappingContext, String tenantIdentifier) {
		this.mappingContext = mappingContext;
		this.tenantIdentifier = tenantIdentifier;
	}

	@Override
	public MappingContextImplementor getMappingContext() {
		return mappingContext;
	}

	@Override
	public String getTenantIdentifier() {
		return tenantIdentifier;
	}
}
