/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.nested;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Attribute {

	@Id
	@GeneratedValue
	private long id;

	@ManyToOne
	private Product product;

	@OneToMany(mappedBy = "attribute", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@IndexedEmbedded
	private List<AttributeValue> values;

	public Attribute() {
		values = new ArrayList<AttributeValue>();
	}

	public Attribute(Product product) {
		this.product = product;
		values = new ArrayList<AttributeValue>();
	}

	public long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public List<AttributeValue> getValues() {
		return values;
	}

	public void setValue(AttributeValue value) {
		values.add( value );
	}
}
