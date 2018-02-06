/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import java.io.Serializable;

public class ISBN implements Serializable {
	private final String stringValue;

	public ISBN(String stringValue) {
		this.stringValue = stringValue;
	}

	public String getStringValue() {
		return stringValue;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ISBN && stringValue.equals( ( (ISBN) obj ).stringValue );
	}

	@Override
	public int hashCode() {
		return stringValue.hashCode();
	}
}
