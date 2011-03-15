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
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

import javax.persistence.*;

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
