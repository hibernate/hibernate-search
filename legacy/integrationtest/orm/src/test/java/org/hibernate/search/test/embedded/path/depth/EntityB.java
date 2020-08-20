/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.path.depth;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Davide D'Alto
 */
@Entity
class EntityB {

	@Id
	@GeneratedValue
	public int id;

	@Field
	public String insideThreshold;

	@OneToOne
	@ContainedIn
	public EntityA a;

	@OneToOne
	@IndexedEmbedded
	public EntityC indexed;

	@OneToOne
	@IndexedEmbedded
	public EntityC skipped;

	public EntityB() {
	}

	public EntityB(EntityC inpath, EntityC indepth) {
		this.indexed = inpath;
		inpath.b = this;

		if ( indepth != null ) {
			this.skipped = indepth;
			indepth.b = this;
		}
	}

}
