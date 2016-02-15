/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.metadata;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Martin Braun
 */
@Indexed
public class RootEntity {

	@DocumentId(name = "MAYBE_ROOT_NOT_NAMED_ID")
	@Field(name = "ROOT_IGNORE_ME")
	private Long rootId;

	@Field(analyze = Analyze.NO)
	private String name;

	@IndexedEmbedded(depth = 2, prefix = "recursiveSelf.", includeEmbeddedObjectId = true)
	private RootEntity anotherRootEntity;

	@IndexedEmbedded(prefix = "otherEntity.", includeEmbeddedObjectId = true)
	private SubEntity someSubEntity;

}
