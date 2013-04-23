/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.bridge;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
@Table(name = "IBT_Entity")
public class IterableBridgeTestEntity {

	static final String NULL_TOKEN = "NULL_MARKER";
	static final String NULL_NUMERIC_TOKEN = "NULL_NUMERIC_MARKER";
	static final String NULL_EMBEDDED = "EMBEDDED_NULL";
	static final String NULL_EMBEDDED_NUMERIC = "EMBEDDED_NUMERIC_NULL";

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

	@Field(indexNullAs = NULL_TOKEN)
	@ElementCollection
	@IndexedEmbedded(indexNullAs = NULL_EMBEDDED)
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

	@Field(store = Store.YES, indexNullAs = NULL_NUMERIC_TOKEN)
	@NumericField
	@ElementCollection
	@IndexedEmbedded(prefix = "embeddedNum", indexNullAs = NULL_EMBEDDED_NUMERIC)
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
	@IndexedEmbedded
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
	@IndexedEmbedded
	@NumericField
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
	@IndexedEmbedded
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
