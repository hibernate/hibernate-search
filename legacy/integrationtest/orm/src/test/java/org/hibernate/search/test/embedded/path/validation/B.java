/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.path.validation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Davide D'Alto
 */
@Entity
public class B {

	@Id
	@GeneratedValue
	public int id;

	@OneToOne(mappedBy = "b")
	@ContainedIn
	public A a;

	@OneToOne(mappedBy = "b2")
	@ContainedIn
	public A a2;

	@OneToOne
	@IndexedEmbedded
	public C c;

	@OneToOne
	@IndexedEmbedded
	public C skipped;

	public B() {
	}

	public B(C c, C skipped) {
		this.c = c;
		c.b = this;

		if ( skipped != null ) {
			this.skipped = skipped;
			skipped.b = this;
		}
	}

}
