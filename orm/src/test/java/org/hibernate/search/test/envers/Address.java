/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.test.envers;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Davide Di Somma <davide.disomma@gmail.com>
 */
@Entity
@Indexed
@Audited(withModifiedFlag = true)
public class Address {
	public Address() {
	}

	public Address(String streetName, Integer houseNumber) {
		super();
		this.streetName = streetName;
		this.houseNumber = houseNumber;
	}

	@Id
	@GeneratedValue
	@DocumentId
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Field
	private String streetName;

	public String getStreetName() {
		return streetName;
	}

	public void setStreetName(String streetName) {
		this.streetName = streetName;
	}

	@Field
	private Integer houseNumber;

	public Integer getHouseNumber() {
		return houseNumber;
	}

	public void setHouseNumber(Integer houseNumber) {
		this.houseNumber = houseNumber;
	}

	@Field
	private Integer flatNumber;

	public Integer getFlatNumber() {
		return flatNumber;
	}

	public void setFlatNumber(Integer flatNumber) {
		this.flatNumber = flatNumber;
	}

	@OneToMany(mappedBy = "address")
	private Set<Person> persons;

	public Set<Person> getPersons() {
		return persons;
	}

	public void setPersons(Set<Person> persons) {
		this.persons = persons;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( flatNumber == null ) ? 0 : flatNumber.hashCode() );
		result = prime * result + ( ( houseNumber == null ) ? 0 : houseNumber.hashCode() );
		result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
		result = prime * result + ( ( persons == null ) ? 0 : persons.hashCode() );
		result = prime * result + ( ( streetName == null ) ? 0 : streetName.hashCode() );
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
		Address other = (Address) obj;
		if ( flatNumber == null ) {
			if ( other.flatNumber != null ) {
				return false;
			}
		}
		else if ( !flatNumber.equals( other.flatNumber ) ) {
			return false;
		}
		if ( houseNumber == null ) {
			if ( other.houseNumber != null ) {
				return false;
			}
		}
		else if ( !houseNumber.equals( other.houseNumber ) ) {
			return false;
		}
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		}
		else if ( !id.equals( other.id ) ) {
			return false;
		}
		if ( persons == null ) {
			if ( other.persons != null ) {
				return false;
			}
		}
		else if ( !persons.equals( other.persons ) ) {
			return false;
		}
		if ( streetName == null ) {
			if ( other.streetName != null ) {
				return false;
			}
		}
		else if ( !streetName.equals( other.streetName ) ) {
			return false;
		}
		return true;
	}
}


