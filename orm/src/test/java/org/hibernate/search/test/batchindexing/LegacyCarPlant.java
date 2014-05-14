/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;


/**
 * @author Bayo Erinle
 */
@Entity
@Table(name = "Z_LEG_CAR_PLANT")
@Indexed
public class LegacyCarPlant {

	private LegacyCarPlantPK id;
	private String name;
	private LegacyCar car;

	@EmbeddedId
	@DocumentId
	@FieldBridge(impl = LegacyCarPlantPKBridge.class)
	public LegacyCarPlantPK getId() {
		return id;
	}

	public void setId(LegacyCarPlantPK id) {
		this.id = id;
	}

	@Column(name = "PLANT_NAME")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@IndexedEmbedded
	@ManyToOne
	@JoinColumn(name = "CAR_ID", insertable = false, updatable = false)
	public LegacyCar getCar() {
		return car;
	}

	public void setCar(LegacyCar car) {
		this.car = car;
	}
}
