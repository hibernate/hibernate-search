/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity(name = FetchSubSelectContainedEntity.NAME)
public class FetchSubSelectContainedEntity {

	public static final String NAME = "contained";

	@Id
	private Integer id;

	@OneToOne(mappedBy = "containedEager")
	private FetchSubSelectIndexedEntity containingEager;

	@ManyToOne
	private FetchSubSelectIndexedEntity containingLazy;

	protected FetchSubSelectContainedEntity() {
	}

	public FetchSubSelectContainedEntity(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}

	public Integer getId() {
		return id;
	}

	public FetchSubSelectIndexedEntity getContainingEager() {
		return containingEager;
	}

	public void setContainingEager(FetchSubSelectIndexedEntity containingEager) {
		this.containingEager = containingEager;
	}

	public FetchSubSelectIndexedEntity getContainingLazy() {
		return containingLazy;
	}

	public void setContainingLazy(FetchSubSelectIndexedEntity containingLazy) {
		this.containingLazy = containingLazy;
	}
}
