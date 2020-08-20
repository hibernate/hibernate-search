/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
