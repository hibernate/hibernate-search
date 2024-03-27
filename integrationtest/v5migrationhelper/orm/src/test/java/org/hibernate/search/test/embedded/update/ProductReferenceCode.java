/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.update;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class ProductReferenceCode {

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne(optional = false)
	private ProductModel model;

	@Column(nullable = false)
	private String rawValue;

	protected ProductReferenceCode() {
	}

	public ProductReferenceCode(ProductModel model, String rawValue) {
		this.model = model;
		this.rawValue = rawValue;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ProductModel getModel() {
		return model;
	}

	public void setModel(ProductModel model) {
		this.model = model;
	}

	public String getRawValue() {
		return rawValue;
	}

	public void setRawValue(String rawValue) {
		this.rawValue = rawValue;
	}

}
