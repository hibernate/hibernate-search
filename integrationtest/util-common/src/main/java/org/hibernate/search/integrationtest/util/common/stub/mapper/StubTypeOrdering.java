/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.mapper;

import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;

class StubTypeOrdering implements IndexableTypeOrdering {
	@Override
	public boolean isSubType(IndexedTypeIdentifier parent, IndexedTypeIdentifier subType) {
		return false;
	}

	@Override
	public Stream<? extends IndexedTypeIdentifier> getAscendingSuperTypes(IndexedTypeIdentifier subType) {
		return Stream.of( subType );
	}

	@Override
	public Stream<? extends IndexedTypeIdentifier> getDescendingSuperTypes(IndexedTypeIdentifier subType) {
		return Stream.of( subType );
	}
}
