/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.session.impl.PojoSearchSessionDelegateImpl;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
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
	public PojoSearchSessionDelegate createSearchSessionDelegate(
			AbstractPojoBackendSessionContext backendSessionContext) {
		return new PojoSearchSessionDelegateImpl(
				indexedTypeManagers, containedTypeManagers,
				backendSessionContext
		);
	}
}
