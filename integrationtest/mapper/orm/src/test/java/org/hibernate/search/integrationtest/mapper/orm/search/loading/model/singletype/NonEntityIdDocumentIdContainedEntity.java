/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

@Entity(name = NonEntityIdDocumentIdContainedEntity.NAME)
public class NonEntityIdDocumentIdContainedEntity {

	public static final String NAME = "NonEntIdDocIdContained";

	@Id
	private Integer id;

	@OneToOne(mappedBy = "containedEager")
	private NonEntityIdDocumentIdIndexedEntity containingEager;

	@ManyToOne
	private NonEntityIdDocumentIdIndexedEntity containingLazy;

	protected NonEntityIdDocumentIdContainedEntity() {
	}

	public NonEntityIdDocumentIdContainedEntity(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}

	public Integer getId() {
		return id;
	}

	public NonEntityIdDocumentIdIndexedEntity getContainingEager() {
		return containingEager;
	}

	public void setContainingEager(NonEntityIdDocumentIdIndexedEntity containingEager) {
		this.containingEager = containingEager;
	}

	public NonEntityIdDocumentIdIndexedEntity getContainingLazy() {
		return containingLazy;
	}

	public void setContainingLazy(NonEntityIdDocumentIdIndexedEntity containingLazy) {
		this.containingLazy = containingLazy;
	}
}
