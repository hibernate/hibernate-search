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
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

/**
 * @author John Griffin
 */
@Entity
@Indexed(index = ElectricalProperties.INDEX_NAME)
public class ElectricalProperties {
	public static final String INDEX_NAME = "elecprops";

	private int id;
	private String content;

	public ElectricalProperties() {

	}

	public ElectricalProperties(int id, String content) {
		this.id = id;
		this.content = content;
	}

	@Field(store = Store.YES, termVector = TermVector.WITH_POSITION_OFFSETS)
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Id
	@DocumentId
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
