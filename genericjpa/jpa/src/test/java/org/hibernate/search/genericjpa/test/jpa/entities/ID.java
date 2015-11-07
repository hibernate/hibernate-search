/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.jpa.entities;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Created by Martin on 25.06.2015.
 */
@Embeddable
public class ID implements Serializable {
	private String firstId;
	private String secondId;

	public ID() {
		this( null, null );
	}

	public ID(String firstId, String secondId) {
		this.firstId = firstId;
		this.secondId = secondId;
	}

	public String getFirstId() {
		return firstId;
	}

	public void setFirstId(String firstId) {
		this.firstId = firstId;
	}

	public String getSecondId() {
		return secondId;
	}

	public void setSecondId(String secondId) {
		this.secondId = secondId;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		ID id = (ID) o;

		if ( firstId != null ? !firstId.equals( id.firstId ) : id.firstId != null ) {
			return false;
		}
		return !(secondId != null ? !secondId.equals( id.secondId ) : id.secondId != null);

	}

	@Override
	public int hashCode() {
		int result = firstId != null ? firstId.hashCode() : 0;
		result = 31 * result + (secondId != null ? secondId.hashCode() : 0);
		return result;
	}
}
