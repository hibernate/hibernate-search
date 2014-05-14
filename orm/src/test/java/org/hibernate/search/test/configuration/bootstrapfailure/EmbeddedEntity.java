/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.bootstrapfailure;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.search.annotations.ContainedIn;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class EmbeddedEntity {
	@Id
	@GeneratedValue
	private long id;

	@ManyToOne
	@ContainedIn
	private IndexedEntity indexedEntity;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public IndexedEntity getIndexedEntity() {
		return indexedEntity;
	}

	public void setIndexedEntity(IndexedEntity indexedEntity) {
		this.indexedEntity = indexedEntity;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "EmbeddedEntity{" );
		sb.append( "id=" ).append( id );
		sb.append( ", indexedEntity=" ).append( indexedEntity );
		sb.append( '}' );
		return sb.toString();
	}
}


