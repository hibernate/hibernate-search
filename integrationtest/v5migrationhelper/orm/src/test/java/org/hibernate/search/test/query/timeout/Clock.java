/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.timeout;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

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
