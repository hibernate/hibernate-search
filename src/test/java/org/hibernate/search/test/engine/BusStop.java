// $Id$
package org.hibernate.search.test.engine;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;

/**
 * Test entity: BusStop is @ContainedIn BusLine
 * 
 * @author Sanne Grinovero
 */
@Entity
public class BusStop {
	
	private Long id;
	private String roadName;
	private Set<BusLine> busses = new HashSet<BusLine>();

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Field
	public String getRoadName() {
		return roadName;
	}

	public void setRoadName(String roadName) {
		this.roadName = roadName;
	}

	@ManyToMany(mappedBy="stops")
	@ContainedIn
	public Set<BusLine> getBusses() {
		return busses;
	}

	public void setBusses(Set<BusLine> busses) {
		this.busses = busses;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((roadName == null) ? 0 : roadName.hashCode());
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
		BusStop other = (BusStop) obj;
		if (roadName == null) {
			if (other.roadName != null)
				return false;
		} else if (!roadName.equals(other.roadName))
			return false;
		return true;
	}
	
}
