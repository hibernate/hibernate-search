/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
@Table(name = "ABT_Entity")
public class ArrayBridgeNullEmbeddedTestEntity {

	static final String NULL_TOKEN = "NULL_MARKER";
	static final String NULL_NUMERIC_TOKEN = "-555";
	static final String NULL_EMBEDDED = "EMBEDDED_NULL";

	static final String NULL_EMBEDDED_NUMERIC = "-666";

	private Long id;
	private String name;
	private Language[] nullIndexed = new Language[0];
	private Integer[] numericNullIndexed = new Integer[0];

	public enum Language {
		ITALIAN, ENGLISH, PIRATE, KLINGON
	}

	@Id
	@GeneratedValue
	@Column(name = "array_id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "name")
	@Field(store = Store.YES)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Field(indexNullAs = NULL_TOKEN, analyze = Analyze.NO)
	@ElementCollection
	/*
	 * This will only have an effect for null maps, since the type for the map values
	 * does not contain any @Field annotation (which means there is nothing to embed).
	 */
	@IndexedEmbedded(indexNullAs = NULL_EMBEDDED)
	@OrderColumn
	@CollectionTable(name = "NullIndexed", joinColumns = @JoinColumn(name = "array_id"))
	@Column(name = "nullIndexed")
	public Language[] getNullIndexed() {
		return nullIndexed;
	}

	public void setNullIndexed(Language[] nullIndexed) {
		this.nullIndexed = nullIndexed;
	}

	@Field(store = Store.YES, indexNullAs = NULL_NUMERIC_TOKEN, analyze = Analyze.NO)
	@ElementCollection
	/*
	 * This will only have an effect for null maps, since the type for the map values
	 * does not contain any @Field annotation (which means there is nothing to embed).
	 */
	@IndexedEmbedded(prefix = "embeddedNum.", indexNullAs = NULL_EMBEDDED_NUMERIC)
	@OrderColumn
	@CollectionTable(name = "NumericNullIndexed", joinColumns = @JoinColumn(name = "array_id"))
	@Column(name = "numericNullIndexed")
	public Integer[] getNumericNullIndexed() {
		return numericNullIndexed;
	}

	public void setNumericNullIndexed(Integer[] phoneNumbers) {
		this.numericNullIndexed = phoneNumbers;
	}

	@Override
	public String toString() {
		return ArrayBridgeNullEmbeddedTestEntity.class.getSimpleName() + "[id=" + id + ", name=" + name + "]";
	}

}
