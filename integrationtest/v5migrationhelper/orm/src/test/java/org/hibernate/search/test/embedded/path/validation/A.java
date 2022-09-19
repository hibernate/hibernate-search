/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.path.validation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
public class A {

	@Id
	@GeneratedValue
	public int id;

	@OneToOne
	@IndexedEmbedded(depth = 0, includePaths = { "c.indexed" })
	public B b;

	@OneToOne
	@IndexedEmbedded(prefix = "prefixed", depth = 0, includePaths = { "c.indexed" })
	public B b2;

}
