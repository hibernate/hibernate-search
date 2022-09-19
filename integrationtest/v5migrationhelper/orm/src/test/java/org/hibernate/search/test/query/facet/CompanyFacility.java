/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

@Entity
public class CompanyFacility {
	@Id
	@GeneratedValue
	private int id;

	@Field(analyze = Analyze.NO)
	@Facet
	private String country;

	@ManyToOne
	@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "companyFacilities")))
	private Company company;

	public CompanyFacility() {
	}

	public CompanyFacility(String country) {
		this.country = country;

	}

	public int getId() {
		return id;
	}

	public Company getCompany() {
		return this.company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	@Override
	public String toString() {
		return "CompanyFacility{" +
				"id=" + id +
				", country='" + country + '\'' +
				'}';
	}
}

