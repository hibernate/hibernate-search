/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
