/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed(index = "AlternateBook")
public class AlternateBook {

	@Id
	@DocumentId
	@Field
	@SortableField
	private Integer id;

	@Field(store = Store.YES)
	private String summary;


	public AlternateBook() {
	}

	public AlternateBook(Integer id, String summary) {
		this.id = id;
		this.summary = summary;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}
}
