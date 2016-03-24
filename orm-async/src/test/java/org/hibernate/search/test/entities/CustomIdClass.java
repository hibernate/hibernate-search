/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.entities;

import java.io.Serializable;

/**
 * @author Martin Braun
 */
public class CustomIdClass implements Serializable {

	private Integer id;
	private Integer id2;

	public CustomIdClass() {

	}

	public CustomIdClass(Integer id, Integer id2) {
		this.id = id;
		this.id2 = id2;
	}

	public Integer getId2() {
		return id2;
	}

	public void setId2(Integer id2) {
		this.id2 = id2;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		CustomIdClass that = (CustomIdClass) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		return !(id2 != null ? !id2.equals( that.id2 ) : that.id2 != null);

	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (id2 != null ? id2.hashCode() : 0);
		return result;
	}
}
