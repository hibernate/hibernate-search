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

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

@Entity(name = NonEntityIdDocumentIdIndexedEntity.NAME)
@Indexed(index = NonEntityIdDocumentIdIndexedEntity.NAME)
@NamedEntityGraph(
		name = NonEntityIdDocumentIdIndexedEntity.GRAPH_EAGER,
		includeAllAttributes = true
)
@NamedEntityGraph(
		name = NonEntityIdDocumentIdIndexedEntity.GRAPH_LAZY
)
public class NonEntityIdDocumentIdIndexedEntity {

	public static final String NAME = "NonEntityIdDocumentId";

	public static final String GRAPH_EAGER = "graph-eager";
	public static final String GRAPH_LAZY = "graph-lazy";

	@Id
	private Integer id;

	@DocumentId
	private Integer documentId;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
	@IndexedEmbedded
	private NonEntityIdDocumentIdContainedEntity containedEager;

	@OneToMany(mappedBy = "containingLazy", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	@IndexedEmbedded
	private List<NonEntityIdDocumentIdContainedEntity> containedLazy = new ArrayList<>();

	protected NonEntityIdDocumentIdIndexedEntity() {
	}

	public NonEntityIdDocumentIdIndexedEntity(int id, int documentId) {
		this.id = id;
		this.documentId = documentId;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}

	public Integer getId() {
		return id;
	}

	public NonEntityIdDocumentIdContainedEntity getContainedEager() {
		return containedEager;
	}

	public void setContainedEager(NonEntityIdDocumentIdContainedEntity containedEager) {
		this.containedEager = containedEager;
	}

	public List<NonEntityIdDocumentIdContainedEntity> getContainedLazy() {
		return containedLazy;
	}
}
