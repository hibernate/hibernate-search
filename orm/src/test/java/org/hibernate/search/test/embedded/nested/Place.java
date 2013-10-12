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
package org.hibernate.search.test.embedded.nested;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Place {
	@Id
	@GeneratedValue
	private Long id;

	@Field
	private String name;

	@OneToOne(cascade = CascadeType.ALL)
	@IndexedEmbedded
	private Address address;

	@ContainedIn
	@ManyToMany(cascade = { CascadeType.ALL }, mappedBy = "placesVisited")
	private Set<Person> visitedBy;

	public Place() {
		this.visitedBy = new HashSet<Person>();
	}

	public Place(String name) {
		this();
		this.name = name;
	}

	public Address getAddress() {
		return address;
	}

	public String getName() {
		return name;
	}

	public Long getId() {
		return id;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public void visitedBy(Person person) {
		visitedBy.add( person );
	}

	public Set<Person> getVisitedBy() {
		return visitedBy;
	}
}
