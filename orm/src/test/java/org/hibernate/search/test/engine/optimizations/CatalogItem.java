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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Proxy;

/**
 * HSEARCH-679 - verify that updates to collections that are not indexed do not trigger indexing.
 *
 * @author Tom Waterhouse
 */
@Entity
@Proxy(lazy = false)
@Table(name = "catalog_item")
public class CatalogItem {

	public CatalogItem() {
	}

	@Id()
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long catalogItemId;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = Item.class)
	@JoinColumn(name = "itemId")
	@LazyToOne(LazyToOneOption.PROXY)
	@NaturalId
	private Item item;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "catalogId")
	@NaturalId
	private Catalog catalog;

	public Long getCatalogItemId() {
		return catalogItemId;
	}

	public void setCatalogItemId(Long catalogItemId) {
		this.catalogItemId = catalogItemId;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public Catalog getCatalog() {
		return catalog;
	}

	public void setCatalog(Catalog catalog) {
		this.catalog = catalog;
	}

}
