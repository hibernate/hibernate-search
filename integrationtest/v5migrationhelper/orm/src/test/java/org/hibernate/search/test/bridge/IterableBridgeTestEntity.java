/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
@Table(name = "IBT_Entity")
public class IterableBridgeTestEntity {

	static final String NULL_LANGUAGE_TOKEN = "PIRATE";
	static final String NULL_NUMERIC_TOKEN = "-555";
	static final int NULL_NUMERIC_TOKEN_INT = -555;

	private Long id;
	private String name;
	private Set<Language> nullIndexed = new HashSet<Language>();
	private List<String> nullNotIndexed = new ArrayList<String>();
	private Set<Integer> numericNullIndexed = new HashSet<Integer>();
	private List<Long> numericNullNotIndexed = new ArrayList<Long>();

	private List<Date> dates = new ArrayList<Date>();

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

	@Field(indexNullAs = NULL_LANGUAGE_TOKEN, analyze = Analyze.NO)
	@ElementCollection
	@CollectionTable(name = "NullIndexed", joinColumns = @JoinColumn(name = "iterable_id"))
	@Column(name = "nullIndexed")
	public Set<Language> getNullIndexed() {
		return nullIndexed;
	}

	public void setNullIndexed(Set<Language> nullIndexed) {
		this.nullIndexed = nullIndexed;
	}

	public void addNullIndexed(Language nullIndexed) {
		this.nullIndexed.add( nullIndexed );
	}

	@Field(store = Store.YES, indexNullAs = NULL_NUMERIC_TOKEN, analyze = Analyze.NO)
	@ElementCollection
	@CollectionTable(name = "NumericNullIndexed", joinColumns = @JoinColumn(name = "iterable_id"))
	@Column(name = "numericNullIndexed")
	public Set<Integer> getNumericNullIndexed() {
		return numericNullIndexed;
	}

	public void setNumericNullIndexed(Set<Integer> phoneNumbers) {
		this.numericNullIndexed = phoneNumbers;
	}

	public void addNumericNullIndexed(Integer number) {
		this.numericNullIndexed.add( number );
	}

	@Field(store = Store.YES)
	@ElementCollection
	@CollectionTable(name = "NullNotIndexed", joinColumns = @JoinColumn(name = "iterable_id"))
	@Column(name = "nullNotIndexed")
	public List<String> getNullNotIndexed() {
		return nullNotIndexed;
	}

	public void setNullNotIndexed(List<String> skipNullCollection) {
		this.nullNotIndexed = skipNullCollection;
	}

	public void addNullNotIndexed(String value) {
		this.nullNotIndexed.add( value );
	}

	@Field(store = Store.YES)
	@ElementCollection
	@CollectionTable(name = "NumericNullNotIndexed", joinColumns = @JoinColumn(name = "iterable_id"))
	@Column(name = "numericNullNotIndexed")
	public List<Long> getNumericNullNotIndexed() {
		return numericNullNotIndexed;
	}

	public void setNumericNullNotIndexed(List<Long> numericSkipNullCollection) {
		this.numericNullNotIndexed = numericSkipNullCollection;
	}

	public void addNumericNullNotIndexed(Long value) {
		this.numericNullNotIndexed.add( value );
	}

	@Field(analyze = Analyze.NO, store = Store.YES)
	@ElementCollection
	@DateBridge(resolution = Resolution.SECOND)
	@CollectionTable(name = "Dates", joinColumns = @JoinColumn(name = "iterable_id"))
	@Column(name = "dates")
	public List<Date> getDates() {
		return dates;
	}

	public void setDates(List<Date> dates) {
		this.dates = dates;
	}

	public void addDate(Date value) {
		this.dates.add( value );
	}

	@Override
	public String toString() {
		return IterableBridgeTestEntity.class.getSimpleName() + "[id=" + id + ", name=" + name + "]";
	}

}
