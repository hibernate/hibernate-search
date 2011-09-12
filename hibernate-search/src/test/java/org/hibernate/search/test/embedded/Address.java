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
package org.hibernate.search.test.embedded;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Target;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Emmanuel Bernard
 */

@Entity
@Indexed
public class Address {
	@Id
	@GeneratedValue
	@DocumentId
	private Long id;

	@Field
	private String street;

	@IndexedEmbedded(depth = 1, prefix = "ownedBy_", targetElement = Owner.class)
	@Target(Owner.class)
	private Person ownedBy;

	@OneToMany(mappedBy = "address")
	@ContainedIn
	private Set<Tower> towers = new HashSet<Tower>();

	@ManyToOne(cascade = CascadeType.ALL)
	@IndexedEmbedded
	private Country country;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public Person getOwnedBy() {
		return ownedBy;
	}

	public void setOwnedBy(Person ownedBy) {
		this.ownedBy = ownedBy;
	}


	public Set<Tower> getTowers() {
		return towers;
	}

	public void setTowers(Set<Tower> towers) {
		this.towers = towers;
	}

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}
}
