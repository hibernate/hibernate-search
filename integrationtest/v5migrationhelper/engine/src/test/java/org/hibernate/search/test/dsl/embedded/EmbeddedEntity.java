/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.dsl.embedded;

import java.util.Date;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;

class EmbeddedEntity {

	@Field
	private String embeddedField;

	@Field(name = "date", analyze = Analyze.NO)
	private Date date;

	public String getEmbeddedField() {
		return embeddedField;
	}

	public void setEmbeddedField(String embeddedField) {
		this.embeddedField = embeddedField;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

}
