/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * Abstract base class for products, having e.g. abstract getCode()
 * returning depending on product type isbn, issn or ean.
 *
 * @author Samppa Saarela
 */
@Entity
public abstract class AbstractProduct {

	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;

	@Field
	private String name;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
	@IndexedEmbedded
	private Set<ProductFeature> features = new HashSet<ProductFeature>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<ProductFeature> getFeatures() {
		return features;
	}

	public void setFeatures(Set<ProductFeature> features) {
		this.features = features;
	}
}
