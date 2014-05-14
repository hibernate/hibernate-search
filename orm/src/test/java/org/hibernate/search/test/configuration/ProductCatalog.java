/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

@Entity
public class ProductCatalog {

	@Id @GeneratedValue
	private Integer id;
	private String name;

	@OneToMany(fetch = FetchType.LAZY)
	@IndexColumn(name = "list_position")
	@Cascade(org.hibernate.annotations.CascadeType.ALL)
	private List<Item> items = new ArrayList<Item>();

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


	public List<Item> getItems() {
		return items;
	}

	public void addItem(Item item) {
		this.items.add( item );
	}
}
