/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.entities;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Martin Braun
 */
@Entity
@Indexed
@Table(name = "TablePerClassOne")
public class TablePerClassOne extends TablePerClass {

	@Field(store = Store.YES)
	private String one;

	public String getOne() {
		return one;
	}

	public void setOne(String one) {
		this.one = one;
	}
}
