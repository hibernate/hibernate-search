/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.batchindexing;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;

import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class AncientBook extends Book {

	public String catalogueGroupName = "";
	public Set<String> alternativeTitles = new HashSet<String>();

	public String getCatalogueGroupName() {
		return catalogueGroupName;
	}

	public void setCatalogueGroupName(String catalogueGroupName) {
		this.catalogueGroupName = catalogueGroupName;
	}

	@ElementCollection(fetch = FetchType.EAGER)
	public Set<String> getAlternativeTitles() {
		return alternativeTitles;
	}

	public void setAlternativeTitles(Set<String> alternativeTitles) {
		this.alternativeTitles = alternativeTitles;
	}

}
