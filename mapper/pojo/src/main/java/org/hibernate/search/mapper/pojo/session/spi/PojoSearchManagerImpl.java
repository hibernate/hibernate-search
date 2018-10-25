/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.spi;

import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.PojoSessionContextImplementor;


public abstract class PojoSearchManagerImpl {

	private final PojoSearchManagerDelegate delegate;

	protected PojoSearchManagerImpl(AbstractBuilder<? extends PojoSearchManagerImpl> builder) {
		this.delegate = builder.mappingDelegate.createSearchManagerDelegate( builder.buildSessionContext() );
	}

	protected final PojoSearchManagerDelegate getDelegate() {
		return delegate;
	}

	protected abstract static class AbstractBuilder<T extends PojoSearchManagerImpl> {

		private final PojoMappingDelegate mappingDelegate;

		public AbstractBuilder(PojoMappingDelegate mappingDelegate) {
			this.mappingDelegate = mappingDelegate;
		}

		protected abstract PojoSessionContextImplementor buildSessionContext();

		public abstract T build();

	}

}
