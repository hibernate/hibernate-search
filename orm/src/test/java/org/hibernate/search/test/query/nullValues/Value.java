/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.nullValues;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedContainer;
import org.hibernate.search.annotations.Store;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Value {
	@Id
	@GeneratedValue
	private int id;

	@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "_custom_token_")
	private String value;

	@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = Field.DEFAULT_NULL_TOKEN)
	private String fallback;

	@Field(analyze = Analyze.NO,
			store = Store.YES,
			indexNullAs = "_dummy_",
			bridge = @FieldBridge(impl = DummyStringBridge.class))
	@Column(name = "dummyvalue")
	private String dummy;

	@ElementCollection
	@IndexedContainer
	@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "_custom_token_")
	private Set<String> containerStringValue;

	@ElementCollection
	@IndexedContainer
	@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "123")
	private Set<Integer> containerNumericValue;

	public Value() {
	}

	public Value(String value) {
		this.value = value;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getFallback() {
		return fallback;
	}

	public void setFallback(String fallback) {
		this.fallback = fallback;
	}

	public String getDummy() {
		return dummy;
	}

	public void setDummy(String dummy) {
		this.dummy = dummy;
	}

	public Set<String> getContainerStringValue() {
		return containerStringValue;
	}

	public void setContainerStringValue(Set<String> containerStringValue) {
		this.containerStringValue = containerStringValue;
	}

	public Set<Integer> getContainerNumericValue() {
		return containerNumericValue;
	}

	public void setContainerNumericValue(Set<Integer> containerValue) {
		this.containerNumericValue = containerValue;
	}
}


