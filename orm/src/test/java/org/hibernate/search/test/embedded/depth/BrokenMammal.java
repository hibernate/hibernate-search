/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.depth;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @IndexedEmbedded pointing to the same class with not depth defined.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
@Indexed
public class BrokenMammal {
	@Id
	public Long id;

	@ManyToOne @IndexedEmbedded
	public BrokenMammal parent;
}
