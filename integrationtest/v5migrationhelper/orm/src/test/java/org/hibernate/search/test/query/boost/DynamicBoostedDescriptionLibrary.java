/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.boost;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.DynamicBoost;

/**
 * Test entity using a custom <code>CustomBoostStrategy</code> to set
 * the document boost as the dynScore field.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
@DynamicBoost(impl = CustomBoostStrategy.class)
@Table(name = "DynBoostLibrary")
public class DynamicBoostedDescriptionLibrary {

	private int id;
	private float dynScore;
	private String name;

	public DynamicBoostedDescriptionLibrary() {
		dynScore = 1.0f;
	}

	@Id
	@GeneratedValue
	@DocumentId
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public float getDynScore() {
		return dynScore;
	}

	public void setDynScore(float dynScore) {
		this.dynScore = dynScore;
	}

	@Field(store = Store.YES)
	@DynamicBoost(impl = CustomFieldBoostStrategy.class)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
