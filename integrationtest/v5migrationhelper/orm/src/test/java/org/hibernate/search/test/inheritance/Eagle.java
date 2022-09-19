/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.inheritance;

import jakarta.persistence.Entity;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Eagle extends Bird {
	private WingType wingType;

	@Field(analyze = Analyze.NO, store = Store.YES)
	public WingType getWingType() {
		return wingType;
	}

	public void setWingType(WingType wingType) {
		this.wingType = wingType;
	}

	public enum WingType {
		BROAD,
		LONG
	}
}
