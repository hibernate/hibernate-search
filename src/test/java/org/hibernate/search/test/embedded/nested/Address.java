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
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Address {
	@Id
	@GeneratedValue
	private Long id;

	@Field(index = Index.TOKENIZED)
	private String street;

	@Field(index = Index.TOKENIZED)
	private String city;

	@ContainedIn
	@OneToMany(mappedBy = "address")
	private Set<Place> places;

	public Address(String street, String city) {
		this();
		this.street = street;
		this.city = city;
	}

	private Address() {
		places = new HashSet<Place>();
	}

	public Long getId() {
		return id;
	}

	public String getStreet() {
		return street;
	}

	public String getCity() {
		return city;
	}

	public Set<Place> getPlaces() {
		return places;
	}

	public void addPlace(Place place) {
		places.add( place );
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public void setCity(String city) {
		this.city = city;
	}
}
