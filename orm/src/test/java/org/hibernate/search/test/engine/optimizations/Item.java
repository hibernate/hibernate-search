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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
@Table(name = "item")
public class Item {

	@Id()
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long itemId;

	@OneToMany(mappedBy = "item", cascade = { CascadeType.REMOVE, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	private Set<CatalogItem> catalogItems = new HashSet<CatalogItem>();

	@Column(length = 255)
	private String name;

	@Column(length = 255)
	private String color;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String kind) {
		this.color = kind;
	}

	public Long getItemId() {
		return itemId;
	}

	public void setItemId(Long doughnutId) {
		this.itemId = doughnutId;
	}

	public Set<CatalogItem> getCatalogItems() {
		return catalogItems;
	}

	public void setCatalogItems(Set<CatalogItem> catalogItems) {
		this.catalogItems = catalogItems;
	}

}
