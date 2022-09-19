/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine.indexapi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.Indexed;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Leaf {

	@Id
	@GeneratedValue
	private int id;

	@ManyToOne
	private Tree tree;

	public int getId() {
		return id;
	}

	public Tree getTree() {
		return tree;
	}
}


