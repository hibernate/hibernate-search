/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
