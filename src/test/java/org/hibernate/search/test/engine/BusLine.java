// $Id$
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

	@Id
	@GeneratedValue 
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Field(index=Index.NO,store=Store.YES)
	public String getBusLineName() {
		return busLineName;
	}

	public void setBusLineName(String busLine) {
		this.busLineName = busLine;
	}

	@ManyToMany(cascade=CascadeType.PERSIST)
	@IndexedEmbedded
	public Set<BusStop> getStops() {
		return stops;
	}
	
	public void setStops(Set<BusStop> stops) {
		this.stops = stops;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((busLineName == null) ? 0 : busLineName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BusLine other = (BusLine) obj;
		if (busLineName == null) {
			if (other.busLineName != null)
				return false;
		} else if (!busLineName.equals(other.busLineName))
			return false;
		return true;
	}
	
}
