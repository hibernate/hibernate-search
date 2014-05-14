/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.polymorphism;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
class DerivedLevel2 extends Level2 {

	@OneToOne
	@ContainedIn
	private Level1 level1Parent;

	public Level1 getLevel1Parent() {
		return level1Parent;
	}

	public void setLevel1Parent(Level1 level1Parent) {
		this.level1Parent = level1Parent;
	}

}
