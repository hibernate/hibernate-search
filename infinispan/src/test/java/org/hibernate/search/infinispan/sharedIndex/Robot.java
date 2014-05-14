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
public class Robot extends Device {

	public Robot() {
		super( "Galactic", "Infinity1000", null );
	}

	public Robot(String serialNumber) {
		super( "Galactic", "Infinity1000", serialNumber );
	}
}
