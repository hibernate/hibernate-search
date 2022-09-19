/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.inheritance;

import java.io.Serializable;
import jakarta.persistence.Entity;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Mammal extends Animal implements Serializable {
	private boolean hasSweatGlands;

	@Field(analyze = Analyze.NO, store = Store.YES)
	public boolean isHasSweatGlands() {
		return hasSweatGlands;
	}

	public void setHasSweatGlands(boolean hasSweatGlands) {
		this.hasSweatGlands = hasSweatGlands;
	}
}
