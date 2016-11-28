/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
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
@Table(name = "IBT_Entity")
public class MapBridgeNullEmbeddedTestEntity {

	static final String NULL_TOKEN = "NULL_MARKER";
	static final String NULL_NUMERIC_TOKEN = "-7777";
	static final String NULL_EMBEDDED = "EMBEDDED_NULL";
	static final String NULL_EMBEDDED_NUMERIC = "-4444";

	private Long id;
	private String name;
	private Map<Integer, Language> nullIndexed = new HashMap<Integer, Language>();
	private Map<Integer, Integer> numericNullIndexed = new HashMap<Integer, Integer>();

	public enum Language {
		ITALIAN, ENGLISH, PIRATE, KLINGON
	}

	@Id
	@GeneratedValue
	@Column(name = "iterable_id")
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
	@CollectionTable(name = "NullIndexed", joinColumns = @JoinColumn(name = "iterable_id"))
	@Column(name = "nullIndexed")
	@MapKeyColumn(nullable = false)
	public Map<Integer, Language> getNullIndexed() {
		return nullIndexed;
	}

	public void setNullIndexed(Map<Integer, Language> nullIndexed) {
		this.nullIndexed = nullIndexed;
	}

	public void addNullIndexed(Integer key, Language nullIndexed) {
		this.nullIndexed.put( key, nullIndexed );
	}

	@Field(store = Store.YES, indexNullAs = NULL_NUMERIC_TOKEN, analyze = Analyze.NO)
	@ElementCollection
	/*
	 * This will only have an effect for null maps, since the type for the map values
	 * does not contain any @Field annotation (which means there is nothing to embed).
	 */
	@IndexedEmbedded(prefix = "embeddedNum.", indexNullAs = NULL_EMBEDDED_NUMERIC)
	@CollectionTable(name = "NumericNullIndexed", joinColumns = @JoinColumn(name = "iterable_id"))
	@Column(name = "numericNullIndexed")
	@MapKeyColumn(nullable = false)
	public Map<Integer, Integer> getNumericNullIndexed() {
		return numericNullIndexed;
	}

	public void setNumericNullIndexed(Map<Integer, Integer> numericNullIndexed) {
		this.numericNullIndexed = numericNullIndexed;
	}

	public void addNumericNullIndexed(Integer key, Integer number) {
		this.numericNullIndexed.put( key, number );
	}

	@Override
	public String toString() {
		return MapBridgeNullEmbeddedTestEntity.class.getSimpleName() + "[id=" + id + ", name=" + name + "]";
	}
}
