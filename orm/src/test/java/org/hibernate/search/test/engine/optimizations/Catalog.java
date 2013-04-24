/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.test.engine.optimizations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Proxy;

/**
 * HSEARCH-679 - verify that updates to collections that are not indexed do not trigger indexing.
 *
 * @author Tom Waterhouse
 */
@Entity
@Proxy(lazy = false)
@Table(name = "catalog")
public class Catalog {

	@Id()
	private Long catalogId;

	@Column(length = 255)
	private String name;

	@OneToMany(mappedBy = "catalog", cascade = { CascadeType.REMOVE, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	private Set<CatalogItem> catalogItems = new HashSet<CatalogItem>();

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "catalogs", cascade = { CascadeType.PERSIST })
	private List<Consumer> consumers = new ArrayList<Consumer>();

	public Catalog() {
	}

	public Long getCatalogId() {
		return catalogId;
	}

	public void setCatalogId(Long catalogId) {
		this.catalogId = catalogId;
	}

	public String getName() {
		return name;
	}

	public void setName(String color) {
		this.name = color;
	}

	public Set<CatalogItem> getCatalogItems() {
		return catalogItems;
	}

	public void setCatalogItems(Set<CatalogItem> catalogItems) {
		this.catalogItems = catalogItems;
	}

	public List<Consumer> getConsumers() {
		return consumers;
	}

	public void setConsumers(List<Consumer> consumers) {
		this.consumers = consumers;
	}

}
