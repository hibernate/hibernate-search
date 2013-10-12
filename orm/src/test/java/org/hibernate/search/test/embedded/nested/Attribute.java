/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.embedded.nested;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.search.annotations.ContainedIn;
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
	@ContainedIn
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
