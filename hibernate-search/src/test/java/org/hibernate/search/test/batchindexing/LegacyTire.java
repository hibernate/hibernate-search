/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.batchindexing;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;

import javax.persistence.*;

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
