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
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author zkurey
 */
@Entity
@Indexed
public class InvalidEmbeddedNonLeafCase {

	@Id
	@GeneratedValue
	public int id;

	@ManyToOne
	@IndexedEmbedded(includePaths = { "emb.e1", // valid
			"emb.e3.c.indexed", // valid
			"emb.e4", // invalid
			"emb.e3" // invalid, not a leaf
	})
	public InvalidEmbeddedWithoutPathsCase e;

}
