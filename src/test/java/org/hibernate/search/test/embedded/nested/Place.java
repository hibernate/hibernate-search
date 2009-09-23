// $Id:$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
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
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Place {
	@Id
	@GeneratedValue
	private Long id;

	@Field(index = Index.TOKENIZED)
	private String name;

	@OneToOne(cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
	@IndexedEmbedded
	private Address address;

	@ContainedIn
	@ManyToMany(cascade = { CascadeType.ALL }, mappedBy = "placesVisited")
	private Set<Person> visitedBy;

	private Place() {
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
