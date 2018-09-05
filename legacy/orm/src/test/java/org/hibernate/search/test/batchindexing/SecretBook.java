/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import javax.persistence.Entity;

/**
 * To cover the unusual case in which a non-indexed entity
 * extends an indexed entity.
 * It shouldn't be possible to find SecretBooks by using a
 * fulltext query.
 *
 * @author Sanne Grinovero
 */
@Entity
public class SecretBook extends Book {

	boolean allCopiesBurnt = true;

	public boolean isAllCopiesBurnt() {
		return allCopiesBurnt;
	}

	public void setAllCopiesBurnt(boolean allCopiesBurnt) {
		this.allCopiesBurnt = allCopiesBurnt;
	}

}
