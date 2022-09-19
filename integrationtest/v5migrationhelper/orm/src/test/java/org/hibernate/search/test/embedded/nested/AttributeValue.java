/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.nested;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Store;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class AttributeValue {

	@Id
	@GeneratedValue
	private long id;

	@ManyToOne(targetEntity = Attribute.class, fetch = FetchType.EAGER)
	private Attribute attribute;

	@Column(name = "att_value")
	@Field(store = Store.YES)
	private String value;

	public AttributeValue() {
	}

	public AttributeValue(Attribute attribute, String value) {
		this.attribute = attribute;
		this.value = value;
	}

	public long getId() {
		return id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Attribute getAttribute() {
		return attribute;
	}

	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}
}
