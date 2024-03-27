/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.loading.mydatastore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MyDatastoreCursor<T> implements AutoCloseable {

	private final Iterator<T> iterator;

	public MyDatastoreCursor(Iterator<T> iterator) {
		this.iterator = iterator;
	}

	@Override
	public void close() {
		// Nothing to do, this class is more or less a stub.
	}

	public List<T> next(int count) {
		if ( !iterator.hasNext() ) {
			return null;
		}
		List<T> result = new ArrayList<>();
		for ( int i = 0; i < count && iterator.hasNext(); i++ ) {
			result.add( iterator.next() );
		}
		return result;
	}

}
