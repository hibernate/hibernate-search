/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity(name = FetchSubSelectIndexedEntity.NAME)
@NamedEntityGraph(
		name = FetchSubSelectIndexedEntity.GRAPH_EAGER,
		includeAllAttributes = true
)
@NamedEntityGraph(
		name = FetchSubSelectIndexedEntity.GRAPH_LAZY
)
public class FetchSubSelectIndexedEntity {

	public static final String NAME = "indexed";

	public static final String GRAPH_EAGER = "graph-eager";
	public static final String GRAPH_LAZY = "graph-lazy";

	@Id
	private Integer id;

	private Integer uniqueProperty;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
	private FetchSubSelectContainedEntity containedEager;

	@OneToMany(mappedBy = "containingLazy", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	@Fetch(FetchMode.SUBSELECT)
	private List<FetchSubSelectContainedEntity> containedLazy = new ArrayList<>();

	protected FetchSubSelectIndexedEntity() {
	}

	public FetchSubSelectIndexedEntity(int id, Integer uniqueProperty) {
		this.id = id;
		this.uniqueProperty = uniqueProperty;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}

	public Integer getId() {
		return id;
	}

	public FetchSubSelectContainedEntity getContainedEager() {
		return containedEager;
	}

	public void setContainedEager(FetchSubSelectContainedEntity containedEager) {
		this.containedEager = containedEager;
	}

	public List<FetchSubSelectContainedEntity> getContainedLazy() {
		return containedLazy;
	}
}
