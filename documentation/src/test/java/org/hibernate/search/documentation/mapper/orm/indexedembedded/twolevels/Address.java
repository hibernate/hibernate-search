/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexedembedded.twolevels;

import javax.persistence.Embeddable;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

// tag::include[]
@Embeddable
public class Address {

	@FullTextField(analyzer = "name") // <4>
	private String country;

	private String city;

	private String street;

	public Address() {
	}

	// Getters and setters
	// ...

	// tag::getters-setters[]
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
	// end::getters-setters[]
}
// end::include[]
