/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.objectloading;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class TestEntity {
	@Id
	private int entityId;

	@DocumentId
	private String documentId;

	private TestEntity() {
		// used by ORM
	}

	public TestEntity(int entityId, String documentId) {
		this.entityId = entityId;
		this.documentId = documentId;
	}

	public int getEntityId() {
		return entityId;
	}

	public String getDocumentId() {
		return documentId;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		TestEntity that = (TestEntity) o;

		if ( entityId != that.entityId ) {
			return false;
		}
		if ( !documentId.equals( that.documentId ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = entityId;
		result = 31 * result + documentId.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "TestEntity{" +
				"entityId=" + entityId +
				", documentId='" + documentId + '\'' +
				'}';
	}
}


