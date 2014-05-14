/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.bootstrapfailure;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class NoSearchEntity {

	@Id
	private Timestamp id;

	public Timestamp getId() {
		return id;
	}

	public void setId(Timestamp id) {
		this.id = id;
	}

}
