/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Test entity using a custom <code>CustomBoostStrategy</code> to set
 * the document boost as the dynScore field.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
@Entity
public class DynamicBoostedDescLibrary {

	@Id
	@GeneratedValue
	private int libraryId;
	private float dynScore;
	private String name;

	public DynamicBoostedDescLibrary() {
		dynScore = 1.0f;
	}


	public int getLibraryId() {
		return libraryId;
	}

	public void setLibraryId(int id) {
		this.libraryId = id;
	}

	public float getDynScore() {
		return dynScore;
	}

	public void setDynScore(float dynScore) {
		this.dynScore = dynScore;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
