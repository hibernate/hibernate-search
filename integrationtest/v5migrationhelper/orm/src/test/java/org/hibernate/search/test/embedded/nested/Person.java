/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.nested;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Person {
	@Id
	@GeneratedValue
	private long id;

	String name;

	@IndexedEmbedded(includeEmbeddedObjectId = true)
	@ManyToMany(cascade = { CascadeType.ALL })
	private List<Place> placesVisited;

	public Person() {
		placesVisited = new ArrayList<Place>( 0 );
	}

	public Person(String name) {
		this();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<Place> getPlacesVisited() {
		return placesVisited;
	}

	public void addPlaceVisited(Place place) {
		placesVisited.add( place );
	}

	public long getId() {
		return id;
	}
}
