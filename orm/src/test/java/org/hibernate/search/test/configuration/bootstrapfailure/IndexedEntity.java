/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.bootstrapfailure;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class IndexedEntity {
	@Id
	@GeneratedValue
	private long id;

	@IndexedEmbedded
	@OneToMany
	private List<EmbeddedEntity> embedded;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<EmbeddedEntity> getEmbedded() {
		return embedded;
	}

	public void setEmbedded(List<EmbeddedEntity> embedded) {
		this.embedded = embedded;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "IndexedEntity{" );
		sb.append( "id=" ).append( id );
		sb.append( ", embedded=" ).append( embedded );
		sb.append( '}' );
		return sb.toString();
	}
}
