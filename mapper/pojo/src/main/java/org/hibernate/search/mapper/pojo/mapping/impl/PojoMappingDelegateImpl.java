/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeContext;
import org.hibernate.search.mapper.pojo.session.impl.PojoSearchSessionDelegateImpl;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.util.common.impl.Closer;


public class PojoMappingDelegateImpl implements PojoMappingDelegate {

	private final PojoIndexedTypeManagerContainer indexedTypeManagers;
	private final PojoContainedTypeManagerContainer containedTypeManagers;

	public PojoMappingDelegateImpl(PojoIndexedTypeManagerContainer indexedTypeManagers,
			PojoContainedTypeManagerContainer containedTypeManagers) {
		this.indexedTypeManagers = indexedTypeManagers;
		this.containedTypeManagers = containedTypeManagers;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( PojoIndexedTypeManager::close, indexedTypeManagers.getAll() );
			closer.pushAll( PojoContainedTypeManager::close, containedTypeManagers.getAll() );
		}
	}

	@Override
	public <E> PojoScopeTypeContext<E> getTypeContext(Class<E> type) {
		Optional<? extends PojoIndexedTypeManager<?, E, ?>> indexedTypeManagerOptional =
				indexedTypeManagers.getByExactClass( type );
		if ( indexedTypeManagerOptional.isPresent() ) {
			return indexedTypeManagerOptional.get();
		}

		Optional<? extends PojoContainedTypeManager<E>> containedTypeManagerOptional =
				containedTypeManagers.getByExactClass( type );
		if ( containedTypeManagerOptional.isPresent() ) {
			return containedTypeManagerOptional.get();
		}

		return null;
	}

	@Override
	public PojoSearchSessionDelegate createSearchSessionDelegate(
			AbstractPojoSessionContextImplementor sessionContextImplementor) {
		return new PojoSearchSessionDelegateImpl(
				indexedTypeManagers, containedTypeManagers,
				sessionContextImplementor
		);
	}
}
