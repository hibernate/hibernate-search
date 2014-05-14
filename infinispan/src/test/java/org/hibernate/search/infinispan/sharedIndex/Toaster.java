/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.infinispan.sharedIndex;

import javax.persistence.Entity;

import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed(index = "device")
public class Toaster extends Device {

	public Toaster() {
		super( "GE", "Scorcher5000", null );
	}

	public Toaster(String serialNumber) {
		super( "GE", "Scorcher5000", serialNumber );
	}
}
