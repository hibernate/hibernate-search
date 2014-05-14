/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * An entity having a property named "id" which is not the identifier.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
@Entity
@Indexed
public class WeirdlyIdentifiedEntity {

	@Id
	@GeneratedValue
	private int testsID;

	private String id;

	public int getTestsID() {
		return this.testsID;
	}

	public void setTestsID(int testsID) {
		this.testsID = testsID;
	}

	@Field
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
