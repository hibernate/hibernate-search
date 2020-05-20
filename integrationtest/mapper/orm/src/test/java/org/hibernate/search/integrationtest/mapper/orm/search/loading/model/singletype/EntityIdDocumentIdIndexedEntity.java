/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

@Entity(name = EntityIdDocumentIdIndexedEntity.NAME)
@Indexed(index = EntityIdDocumentIdIndexedEntity.NAME)
@NamedEntityGraph(
		name = EntityIdDocumentIdIndexedEntity.GRAPH_EAGER,
		includeAllAttributes = true
)
@NamedEntityGraph(
		name = EntityIdDocumentIdIndexedEntity.GRAPH_LAZY
)
public class EntityIdDocumentIdIndexedEntity {

	public static final String NAME = "EntityIdDocumentId";

	public static final String GRAPH_EAGER = "graph-eager";
	public static final String GRAPH_LAZY = "graph-lazy";

	@Id
	private Integer id;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
	@IndexedEmbedded
	private EntityIdDocumentIdContainedEntity containedEager;

	@OneToMany(mappedBy = "containingLazy", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	@IndexedEmbedded
	private List<EntityIdDocumentIdContainedEntity> containedLazy = new ArrayList<>();

	protected EntityIdDocumentIdIndexedEntity() {
	}

	public EntityIdDocumentIdIndexedEntity(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}

	public Integer getId() {
		return id;
	}

	public EntityIdDocumentIdContainedEntity getContainedEager() {
		return containedEager;
	}

	public void setContainedEager(EntityIdDocumentIdContainedEntity containedEager) {
		this.containedEager = containedEager;
	}

	public List<EntityIdDocumentIdContainedEntity> getContainedLazy() {
		return containedLazy;
	}
}
