/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.dsl.embedded;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.test.bridge.PaddedIntegerBridge;

@Embeddable
public class EmbeddedEntity {

	@Field
	private String embeddedField;

	@Field(name = "num", analyze = Analyze.NO)
	@FieldBridge(
		impl = PaddedIntegerBridge.class,
		params = { @Parameter(name = "padding", value = "4") }
	)

	@Column(name = "num")
	private Integer number;

	public String getEmbeddedField() {
		return embeddedField;
	}

	public void setEmbeddedField(String embeddedField) {
		this.embeddedField = embeddedField;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

}
