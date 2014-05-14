/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;

/**
 * @author Bayo Erinle
 */
@Entity
@Table(name = "Z_LEG_TIRE")
public class LegacyTire {

	private LegacyTirePK id;
	private int tireSize;
	private LegacyCar car;

	@Field
	@Column(name = "TIRE_SIZE")
	public int getTireSize() {
		return tireSize;
	}

	public void setTireSize(int tireSize) {
		this.tireSize = tireSize;
	}

	@ManyToOne
	@JoinColumn(name = "CAR_ID", insertable = false, updatable = false)
	public LegacyCar getCar() {
		return car;
	}

	public void setCar(LegacyCar car) {
		this.car = car;
	}

	@Id
	@DocumentId
	@FieldBridge(impl = LegacyTirePKBridge.class)
	public LegacyTirePK getId() {
		return id;
	}

	public void setId(LegacyTirePK id) {
		this.id = id;
	}
}
