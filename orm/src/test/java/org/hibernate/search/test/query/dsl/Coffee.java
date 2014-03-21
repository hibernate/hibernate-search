/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.query.dsl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
@Indexed
public class Coffee {

	@Id
	@GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) {
		this.id = id;
	}
	private Integer id;

	@Field(termVector = TermVector.NO, store = Store.YES)
	public String getName() { return name; }
	public void setName(String name) { this.name = name; };
	private String name;

	@Field(termVector = TermVector.YES)
	public String getSummary() { return summary; }
	public void setSummary(String summary) { this.summary = summary; }
	private String summary;

	@Column(length = 2000)
	@Field(termVector = TermVector.YES)
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	private String description;

	public int getIntensity() { return intensity; }
	public void setIntensity(int intensity) { this.intensity = intensity; }
	private int intensity;

	// Not stored nor term vector, i.e. cannot be used for More Like This
	@Field
	public String getInternalDescription() { return internalDescription; }
	public void setInternalDescription(String internalDescription) { this.internalDescription = internalDescription; }
	private String internalDescription;

	@ManyToOne
	@IndexedEmbedded
	public CoffeeBrand getBrand() { return brand; }
	public void setBrand(CoffeeBrand brand) { this.brand = brand; }
	private CoffeeBrand brand;

	@Override
	public String toString() {
		return "Coffee{" +
				"id=" + id +
				", name='" + name + '\'' +
				", summary='" + summary + '\'' +
				", description='" + description + '\'' +
				", intensity=" + intensity +
				'}';
	}
}
