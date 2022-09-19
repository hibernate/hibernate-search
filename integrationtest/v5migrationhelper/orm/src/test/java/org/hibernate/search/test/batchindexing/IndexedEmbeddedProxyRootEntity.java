/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.batchindexing;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;

@Entity
@Indexed
public class IndexedEmbeddedProxyRootEntity {

	@Id
	@GeneratedValue
	private Integer id;

	@OneToOne(optional = false, fetch = FetchType.LAZY)
	@IndexedEmbedded
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	private IndexedEmbeddedProxyLazyEntity lazyEntity;

	@OneToOne(optional = false, fetch = FetchType.LAZY)
	@IndexedEmbedded
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	private IndexedEmbeddedProxyLazyEntity lazyEntity2;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public IndexedEmbeddedProxyLazyEntity getLazyEntity() {
		return lazyEntity;
	}

	public void setLazyEntity(IndexedEmbeddedProxyLazyEntity lazyEntity) {
		this.lazyEntity = lazyEntity;
	}

	public IndexedEmbeddedProxyLazyEntity getLazyEntity2() {
		return lazyEntity2;
	}

	public void setLazyEntity2(IndexedEmbeddedProxyLazyEntity lazyEntity) {
		this.lazyEntity2 = lazyEntity;
	}

}
