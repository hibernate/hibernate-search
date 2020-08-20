/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
