/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

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
	private String serviceComments = "nothing";
	private Date startingDate = new Date();
	private transient int numMethodCalls = 0;

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

	@ManyToMany(mappedBy = "stops", cascade = CascadeType.ALL)
	@ContainedIn
	public Set<BusLine> getBusses() {
		return busses;
	}

	public void setBusses(Set<BusLine> busses) {
		this.busses = busses;
	}

	public String getServiceComments() {
		return serviceComments;
	}

	public void setServiceComments(String serviceComments) {
		this.serviceComments = serviceComments;
	}

	@Field
	public Date getStartingDate() {
		return startingDate;
	}

	public void setStartingDate(Date startingDate) {
		this.startingDate = startingDate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ( ( roadName == null ) ? 0 : roadName.hashCode() );
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
		BusStop other = (BusStop) obj;
		if ( roadName == null ) {
			if ( other.roadName != null ) {
				return false;
			}
		}
		else if ( !roadName.equals( other.roadName ) ) {
			return false;
		}
		return true;
	}

	@Transient
	public int getNumMethodCalls() {
		return numMethodCalls;
	}

	public void setNumMethodCalls(int numMethodCalls) {
		this.numMethodCalls = numMethodCalls;
	}
}
