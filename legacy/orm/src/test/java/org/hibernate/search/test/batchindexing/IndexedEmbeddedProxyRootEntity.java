/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.batchindexing;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

@Entity
@Indexed
public class IndexedEmbeddedProxyRootEntity {

	@Id
	@GeneratedValue
	private Integer id;

	@OneToOne(optional = false, fetch = FetchType.LAZY)
	@IndexedEmbedded
	private IndexedEmbeddedProxyLazyEntity lazyEntity;

	@OneToOne(optional = false, fetch = FetchType.LAZY)
	@IndexedEmbedded
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
