/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

@Entity(name = EntityIdDocumentIdContainedEntity.NAME)
public class EntityIdDocumentIdContainedEntity {

	public static final String NAME = "EntIdDocIdContained";

	@Id
	private Integer id;

	@OneToOne(mappedBy = "containedEager", fetch = FetchType.LAZY)
	private EntityIdDocumentIdIndexedEntity containingEager;

	@ManyToOne
	private EntityIdDocumentIdIndexedEntity containingLazy;

	protected EntityIdDocumentIdContainedEntity() {
	}

	public EntityIdDocumentIdContainedEntity(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}

	public Integer getId() {
		return id;
	}

	public EntityIdDocumentIdIndexedEntity getContainingEager() {
		return containingEager;
	}

	public void setContainingEager(EntityIdDocumentIdIndexedEntity containingEager) {
		this.containingEager = containingEager;
	}

	public EntityIdDocumentIdIndexedEntity getContainingLazy() {
		return containingLazy;
	}

	public void setContainingLazy(EntityIdDocumentIdIndexedEntity containingLazy) {
		this.containingLazy = containingLazy;
	}
}
