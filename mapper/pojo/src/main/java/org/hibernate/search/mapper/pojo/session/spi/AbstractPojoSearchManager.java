/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.spi;

import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;


public abstract class AbstractPojoSearchManager {

	private final PojoSearchManagerDelegate delegate;

	protected AbstractPojoSearchManager(AbstractBuilder<? extends AbstractPojoSearchManager> builder) {
		this.delegate = builder.mappingDelegate.createSearchManagerDelegate( builder.buildSessionContext() );
	}

	protected final PojoSearchManagerDelegate getDelegate() {
		return delegate;
	}

	protected abstract static class AbstractBuilder<T extends AbstractPojoSearchManager> {

		private final PojoMappingDelegate mappingDelegate;

		public AbstractBuilder(PojoMappingDelegate mappingDelegate) {
			this.mappingDelegate = mappingDelegate;
		}

		protected abstract AbstractPojoSessionContextImplementor buildSessionContext();

		public abstract T build();

	}

}
