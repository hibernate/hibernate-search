/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.nested;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.search.annotations.Field;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Address {
	@Id
	@GeneratedValue
	private Long id;

	@Field
	private String street;

	@Field
	private String city;

	@OneToMany(mappedBy = "address")
	private Set<Place> places;

	public Address(String street, String city) {
		this();
		this.street = street;
		this.city = city;
	}

	public Address() {
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
