/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.Field;

/**
 * A mapping from product to its "features"
 *
 * @author Samppa Saarela
 */
@Entity
public class ProductFeature {

	@Id
	@GeneratedValue
	private Integer id;

	@Field
	private String name;

	@ManyToOne
	private AbstractProduct product;

	// Some other fields justifying this class as an entity...

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

	public AbstractProduct getProduct() {
		return product;
	}

	public void setProduct(AbstractProduct product) {
		this.product = product;
	}
}
