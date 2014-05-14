/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.query.dsl;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
public class CoffeeBrand {

	@Id
	@GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	private Integer id;

	@Field(termVector = TermVector.YES)
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	private String name;

	@Field(termVector = TermVector.NO, store = Store.COMPRESS)
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	private String description;
}
