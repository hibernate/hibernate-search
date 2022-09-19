/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

@Entity
@Indexed
public class Company {
	@Id
	@GeneratedValue
	private int id;

	private String companyName;

	@IndexedEmbedded
	@OneToMany(cascade = CascadeType.ALL)
	private Set<CompanyFacility> companyFacilities = new HashSet<>();

	public Company() {
	}

	public Company(String companyName) {
		this.companyName = companyName;
	}

	public int getId() {
		return id;
	}

	public String getMake() {
		return companyName;
	}

	public void addCompanyFacility(CompanyFacility companyFacility) {
		this.companyFacilities.add( companyFacility );
	}

	@Override
	public String toString() {
		return "Company{" +
				"id=" + id +
				", companyName='" + companyName + '\'' +
				", companyFacilities=" + companyFacilities +
				'}';
	}
}

