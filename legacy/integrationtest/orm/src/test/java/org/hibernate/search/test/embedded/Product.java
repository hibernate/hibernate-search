/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.CascadeType;
import javax.persistence.MapKeyColumn;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.DocumentId;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Product {

	@Id @GeneratedValue @DocumentId
	private Integer id;

	@Field
	private String name;

	@ManyToMany(cascade = CascadeType.REMOVE) //just to make the test easier, cascade doesn't really make any business sense
	@IndexedEmbedded
	private Set<Author> authors = new HashSet<Author>();

	@ManyToMany(cascade = CascadeType.REMOVE) //just to make the test easier, cascade doesn't really make any business sense
	@MapKeyColumn(name = "CUST_NAME", nullable = false)
	@IndexedEmbedded
	private Map<String, Order> orders = new HashMap<String, Order>();

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

	public Set<Author> getAuthors() {
		return authors;
	}

	public void setAuthors(Set<Author> authors) {
		this.authors = authors;
	}

	public Map<String, Order> getOrders() {
		return orders;
	}

	public void setOrders(Map<String, Order> orders) {
		this.orders = orders;
	}
}
