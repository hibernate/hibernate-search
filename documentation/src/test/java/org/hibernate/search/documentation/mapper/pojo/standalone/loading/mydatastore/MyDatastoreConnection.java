/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.loading.mydatastore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MyDatastoreConnection implements AutoCloseable {

	private final MyDatastore datastore;

	public MyDatastoreConnection(MyDatastore datastore) {
		this.datastore = datastore;
	}

	@Override
	public void close() {
		// Nothing to do, this class is more or less a stub.
	}

	public MyDatastoreCursor<String> scrollIdentifiers(Collection<? extends Class<?>> typeFilter) {
		if ( typeFilter.size() != 1 ) {
			throw new IllegalArgumentException( "This implementation only supports targeting one type at a time" );
		}
		return new MyDatastoreCursor<>( datastore.entities.get( typeFilter.iterator().next() ).keySet().iterator() );
	}

	public long countEntities(Collection<? extends Class<?>> typeFilter) {
		if ( typeFilter.size() != 1 ) {
			throw new IllegalArgumentException( "This implementation only supports targeting one type at a time" );
		}
		return datastore.entities.get( typeFilter.iterator().next() ).keySet().size();
	}

	public <T> List<T> loadEntitiesById(Class<T> entityType, List<String> identifiers) {
		return loadEntitiesByIdInSameOrder( entityType, identifiers );
	}

	public <T> List<T> loadEntitiesByIdInSameOrder(Class<T> entityType, List<?> identifiers) {
		Map<String, ?> entities = datastore.entities.get( entityType );
		return identifiers.stream()
				.map( id -> entityType.cast( entities.get( (String) id ) ) )
				.collect( Collectors.toList() );
	}
}
