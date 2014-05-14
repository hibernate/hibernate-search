/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Bayo Erinle
 */
@Entity
@Indexed
@Table(name = "Z_LEG_CAR")
public class LegacyCar {

	private String id;
	private String model;
	private Set<LegacyTire> tires = new HashSet<LegacyTire>();

	@Field
	@Column(name = "MODEL")
	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	@IndexedEmbedded
	@OneToMany(mappedBy = "car", cascade = CascadeType.ALL)
	public Set<LegacyTire> getTires() {
		return tires;
	}

	public void setTires(Set<LegacyTire> tires) {
		this.tires = tires;
	}

	@Id
	@DocumentId
	@Column(name = "CAR_ID")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
