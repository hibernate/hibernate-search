/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend;

import java.util.Objects;

import org.hibernate.search.engine.backend.common.DocumentReference;

public class StubDocumentReference implements DocumentReference {

	private final String indexName;
	private final String id;

	public StubDocumentReference(String indexName, String id) {
		this.indexName = indexName;
		this.id = id;
	}

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" +
				"indexName='" + indexName +
				", id=" + id +
				"]";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		StubDocumentReference that = (StubDocumentReference) o;
		return Objects.equals( indexName, that.indexName ) &&
				Objects.equals( id, that.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( indexName, id );
	}
}
