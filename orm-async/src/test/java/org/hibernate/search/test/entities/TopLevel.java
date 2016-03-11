/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.entities;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import java.util.Set;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Martin Braun
 */
@Entity
@Indexed
@Table(name = "toplevel")
public class TopLevel {

	@Id
	private String id;

	@ElementCollection
	@CollectionTable(name = "toplevel_embedded", joinColumns = @JoinColumn(name = "id"))
	@IndexedEmbedded
	private Set<Embedded> embedded;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Set<Embedded> getEmbedded() {
		return embedded;
	}

	public void setEmbedded(Set<Embedded> embedded) {
		this.embedded = embedded;
	}
}
