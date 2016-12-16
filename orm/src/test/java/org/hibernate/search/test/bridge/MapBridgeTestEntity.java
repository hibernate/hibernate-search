/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.Date;
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
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
@Table(name = "IBT_Entity")
public class MapBridgeTestEntity {

	static final String NULL_TOKEN = "NULL_MARKER";
	static final String NULL_NUMERIC_TOKEN = "-7777";

	private Long id;
	private String name;
	private Map<Integer, Language> nullIndexed = new HashMap<Integer, Language>();
	private Map<Integer, String> nullNotIndexed = new HashMap<Integer, String>();
	private Map<Integer, Integer> numericNullIndexed = new HashMap<Integer, Integer>();
	private Map<Integer, Long> numericNullNotIndexed = new HashMap<Integer, Long>();

	private Map<Integer, Date> dates = new HashMap<Integer, Date>();

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
	@IndexedEmbedded
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
	@IndexedEmbedded
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

	@Field(store = Store.YES)
	@ElementCollection
	@IndexedEmbedded
	@CollectionTable(name = "NullNotIndexed", joinColumns = @JoinColumn(name = "iterable_id"))
	@Column(name = "nullNotIndexed")
	@MapKeyColumn(nullable = false)
	public Map<Integer, String> getNullNotIndexed() {
		return nullNotIndexed;
	}

	public void setNullNotIndexed(Map<Integer, String> nullNotIndexed) {
		this.nullNotIndexed = nullNotIndexed;
	}

	public void addNullNotIndexed(Integer key, String value) {
		this.nullNotIndexed.put( key, value );
	}

	@Field(store = Store.YES)
	@ElementCollection
	@IndexedEmbedded
	@CollectionTable(name = "NumericNullNotIndexed", joinColumns = @JoinColumn(name = "iterable_id"))
	@Column(name = "numericNullNotIndexed")
	@MapKeyColumn(nullable = false)
	public Map<Integer, Long> getNumericNullNotIndexed() {
		return numericNullNotIndexed;
	}

	public void setNumericNullNotIndexed(Map<Integer, Long> numericSkipNullCollection) {
		this.numericNullNotIndexed = numericSkipNullCollection;
	}

	public void addNumericNullNotIndexed(Integer key, Long value) {
		this.numericNullNotIndexed.put( key, value );
	}

	@Field(analyze = Analyze.NO, store = Store.YES)
	@ElementCollection
	@IndexedEmbedded
	@DateBridge(resolution = Resolution.SECOND)
	@CollectionTable(name = "Dates", joinColumns = @JoinColumn(name = "iterable_id"))
	@Column(name = "dates")
	@MapKeyColumn(nullable = false)
	public Map<Integer, Date> getDates() {
		return dates;
	}

	public void setDates(Map<Integer, Date> dates) {
		this.dates = dates;
	}

	public void addDate(Integer key, Date value) {
		this.dates.put( key, value );
	}

	@Override
	public String toString() {
		return MapBridgeTestEntity.class.getSimpleName() + "[id=" + id + ", name=" + name + "]";
	}
}
