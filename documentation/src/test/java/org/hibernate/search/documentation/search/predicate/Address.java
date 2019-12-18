/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.predicate;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

@Embeddable
public class Address {

	@FullTextField(analyzer = "name") // <4>
	private String country;

	private String city;

	private String street;

	@Embedded
	@GenericField
	private EmbeddableGeoPoint coordinates;

	public Address() {
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public EmbeddableGeoPoint getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(EmbeddableGeoPoint coordinates) {
		this.coordinates = coordinates;
	}
}
