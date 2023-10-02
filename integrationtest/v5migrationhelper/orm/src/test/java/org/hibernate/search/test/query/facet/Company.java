/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.facet;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

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

