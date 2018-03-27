/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.mapper.pojo.mapping.PojoMapping;

public abstract class PojoMappingImpl<M extends PojoMapping> implements PojoMapping, MappingImplementor<M> {

	private final PojoMappingDelegate delegate;

	public PojoMappingImpl(PojoMappingDelegate delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close() {
		delegate.close();
	}

	protected final PojoMappingDelegate getDelegate() {
		return delegate;
	}

	@Override
	public boolean isIndexable(Class<?> type) {
		return delegate.isIndexable( type );
	}

	@Override
	public boolean isSearchable(Class<?> type) {
		return delegate.isSearchable( type );
	}
}
