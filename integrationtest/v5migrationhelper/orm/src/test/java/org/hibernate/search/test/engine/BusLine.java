/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

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
