/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchManagerDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.PojoSessionContextImplementor;
import org.hibernate.search.util.impl.common.Closer;


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
	public boolean isWorkable(Class<?> type) {
		return indexedTypeManagers.getByExactClass( type ).isPresent()
				|| containedTypeManagers.getByExactClass( type ).isPresent();
	}

	@Override
	public boolean isIndexable(Class<?> type) {
		return indexedTypeManagers.getByExactClass( type ).isPresent();
	}

	@Override
	public boolean isSearchable(Class<?> type) {
		return indexedTypeManagers.getAllBySuperClass( type ).isPresent();
	}

	@Override
	public PojoSearchManagerDelegate createSearchManagerDelegate(PojoSessionContextImplementor sessionContextImplementor) {
		return new PojoSearchManagerDelegateImpl(
				indexedTypeManagers, containedTypeManagers,
				sessionContextImplementor
		);
	}
}
