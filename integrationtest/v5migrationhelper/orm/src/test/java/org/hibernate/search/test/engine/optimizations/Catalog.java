/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
