/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.engine;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

/**
 * Test entity: BusLine have many BusStops: needed to verify
 * indexing of a lazy-loaded collection in out-of-transaction use case.
 *
 * @author Sanne Grinovero
 */
@Entity
@Indexed
public class BusLine {

	private Long id;
	private String busLineName;
	private Set<BusStop> stops = new HashSet<BusStop>();
	private Integer busLineCode = Integer.valueOf( 0 );
	private boolean operating = true;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Field(index = Index.NO, store = Store.YES)
	public String getBusLineName() {
		return busLineName;
	}

	public void setBusLineName(String busLine) {
		this.busLineName = busLine;
	}

	@ManyToMany(cascade = CascadeType.ALL)
	@IndexedEmbedded
	public Set<BusStop> getStops() {
		return stops;
	}

	public void setStops(Set<BusStop> stops) {
		this.stops = stops;
	}

	public Integer getBusLineCode() {
		return busLineCode;
	}

	public void setBusLineCode(Integer busLineCode) {
		this.busLineCode = busLineCode;
	}

	@Field
	public boolean isOperating() {
		return operating;
	}

	public void setOperating(boolean operating) {
		this.operating = operating;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( busLineName == null ) ? 0 : busLineName.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		BusLine other = (BusLine) obj;
		if ( busLineName == null ) {
			if ( other.busLineName != null ) {
				return false;
			}
		}
		else if ( !busLineName.equals( other.busLineName ) ) {
			return false;
		}
		return true;
	}

}
