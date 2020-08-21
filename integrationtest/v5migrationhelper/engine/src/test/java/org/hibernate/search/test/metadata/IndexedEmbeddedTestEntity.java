/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.metadata;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

@Indexed
public class IndexedEmbeddedTestEntity {

	@DocumentId
	private Long id;

	@Field(analyze = Analyze.NO)
	private String name;

	@IndexedEmbedded(includePaths = "name")
	private IndexedEmbeddedTestEntity indexedEmbeddedWithIncludePath;

	@IndexedEmbedded(depth = 1)
	private IndexedEmbeddedTestEntity indexedEmbeddedWithDepth;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public IndexedEmbeddedTestEntity getIndexedEmbeddedWithIncludePath() {
		return indexedEmbeddedWithIncludePath;
	}

	public void setIndexedEmbeddedWithIncludePath(IndexedEmbeddedTestEntity indexedEmbeddedWithIncludePath) {
		this.indexedEmbeddedWithIncludePath = indexedEmbeddedWithIncludePath;
	}

	public IndexedEmbeddedTestEntity getIndexedEmbeddedWithDepth() {
		return indexedEmbeddedWithDepth;
	}

	public void setIndexedEmbeddedWithDepth(IndexedEmbeddedTestEntity indexedEmbeddedWithDepth) {
		this.indexedEmbeddedWithDepth = indexedEmbeddedWithDepth;
	}

}
