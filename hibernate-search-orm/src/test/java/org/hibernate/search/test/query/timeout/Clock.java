/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.query.timeout;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Clock {
	private Long id;
	private String model;
	private String brand;
	private Long durability;

	public Clock() {
	}

	public Clock(Long id, String model, String brand, Long durability) {
		this.id = id;
		this.model = model;
		this.brand = brand;
		this.durability = durability;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}


	@Field
	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}


	@Field
	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	@Field
	public Long getDurability() {
		return durability;
	}

	public void setDurability(Long durability) {
		this.durability = durability;
	}
}
