/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.batchindexing;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class ConcurrentData {
	private Long id;
	private String data;

	public ConcurrentData() {
	}

	public ConcurrentData(String data) {
		this.data = data;
	}

	@Id
	@GeneratedValue
	@DocumentId
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Field
	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
