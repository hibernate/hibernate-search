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
import org.hibernate.search.annotations.IndexedEmbedded;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Bayo Erinle
 */
@Entity
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
