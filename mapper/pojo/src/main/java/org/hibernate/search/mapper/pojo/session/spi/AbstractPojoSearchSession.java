/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.spi;

import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;


public abstract class AbstractPojoSearchSession {

	private final PojoSearchSessionDelegate delegate;

	protected AbstractPojoSearchSession(AbstractBuilder<? extends AbstractPojoSearchSession> builder,
			AbstractPojoBackendSessionContext backendSessionContext) {
		this.delegate = builder.mappingDelegate.createSearchSessionDelegate( backendSessionContext );
	}

	protected final PojoSearchSessionDelegate getDelegate() {
		return delegate;
	}

	protected abstract static class AbstractBuilder<T extends AbstractPojoSearchSession> {

		private final PojoMappingDelegate mappingDelegate;

		public AbstractBuilder(PojoMappingDelegate mappingDelegate) {
			this.mappingDelegate = mappingDelegate;
		}

		public abstract T build();

	}

}
