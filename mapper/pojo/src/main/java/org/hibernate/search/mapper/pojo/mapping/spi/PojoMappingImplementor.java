/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.mapper.pojo.mapping.PojoMapping;
import org.hibernate.search.util.impl.common.Closer;

public abstract class PojoMappingImplementor<M extends PojoMapping> implements PojoMapping, MappingImplementor<M> {

	private final PojoMappingDelegate delegate;

	private final List<CloseDelegate> closeDelegates = new ArrayList<>();

	private boolean closed = false;

	public PojoMappingImplementor(PojoMappingDelegate delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close() {
		if ( !closed ) {
			// Make sure to avoid infinite recursion when one of the delegates calls this.close()
			closed = true;
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.push( PojoMappingDelegate::close, delegate );
				closer.pushAll( CloseDelegate::close, closeDelegates );
			}
		}
	}

	protected final PojoMappingDelegate getDelegate() {
		return delegate;
	}

	@Override
	public boolean isWorkable(Class<?> type) {
		return delegate.isWorkable( type );
	}

	@Override
	public boolean isIndexable(Class<?> type) {
		return delegate.isIndexable( type );
	}

	@Override
	public boolean isSearchable(Class<?> type) {
		return delegate.isSearchable( type );
	}

	public void onClose(CloseDelegate closeable) {
		closeDelegates.add( closeable );
	}

	public interface CloseDelegate extends AutoCloseable {
		@Override
		void close();
	}
}
