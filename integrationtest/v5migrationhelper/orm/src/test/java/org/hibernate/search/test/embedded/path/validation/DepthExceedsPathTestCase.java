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
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;

/**
 * @author zkurey
 */
@Entity
@Indexed
public class DepthExceedsPathTestCase {

	@Id
	@GeneratedValue
	public int id;

	@ManyToOne
	@IndexedEmbedded(depth = 6, includePaths = { "a.b.c.indexed" })
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	public ReferencesIndexedEmbeddedA e;

}
