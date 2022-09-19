/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
