/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.inheritance;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Store;

/**
 * @author Hardy Ferentschik
 */
public abstract class BaseClass {

	private Integer id;

	protected String name;

	public BaseClass(Integer id) {
		this.id = id;
	}

	@DocumentId
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}


	@Field(name = "name", store = Store.YES)
	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}
}
