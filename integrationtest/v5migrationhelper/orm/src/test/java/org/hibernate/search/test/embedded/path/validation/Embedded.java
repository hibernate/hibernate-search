/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.path.validation;

import java.util.Set;

import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;

@Embeddable
public class Embedded {

	@Field
	public String e1;

	@jakarta.persistence.ElementCollection
	public Set<Integer> e2;

	@IndexedEmbedded
	@OneToMany
	public Set<B> e3;

}
