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
