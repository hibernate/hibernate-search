/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;

import org.hibernate.search.mapper.pojo.extractor.builtin.MapKeyExtractor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerValueExtractorBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

/**
 * Test automatic indexing based on Hibernate ORM entity events.
 *
 * This test only checks updates involving a multi-valued, Map keys association.
 * Other tests in the same package check more basic, direct updates or updates involving different associations.
 */
public class OrmAutomaticIndexingMapKeysAssociationIT extends AbstractOrmAutomaticIndexingMultiAssociationIT<
		OrmAutomaticIndexingMapKeysAssociationIT.IndexedEntity,
		OrmAutomaticIndexingMapKeysAssociationIT.ContainingEntity,
		OrmAutomaticIndexingMapKeysAssociationIT.ContainedEntity,
		Map<OrmAutomaticIndexingMapKeysAssociationIT.ContainedEntity, String>,
		List<OrmAutomaticIndexingMapKeysAssociationIT.ContainingEntity>
		> {

	public OrmAutomaticIndexingMapKeysAssociationIT() {
		super( new MapKeysModelPrimitives() );
	}

	private static class MapKeysModelPrimitives
			implements ModelPrimitives<IndexedEntity, ContainingEntity, ContainedEntity,
			Map<ContainedEntity, String>, List<ContainingEntity>> {

		@Override
		public String getIndexName() {
			return IndexedEntity.INDEX;
		}

		@Override
		public Class<IndexedEntity> getIndexedClass() {
			return IndexedEntity.class;
		}

		@Override
		public Class<ContainingEntity> getContainingClass() {
			return ContainingEntity.class;
		}

		@Override
		public Class<ContainedEntity> getContainedClass() {
			return ContainedEntity.class;
		}

		@Override
		public IndexedEntity newIndexed(int id) {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			return entity;
		}

		@Override
		public ContainingEntity newContaining(int id) {
			ContainingEntity entity = new ContainingEntity();
			entity.setId( id );
			return entity;
		}

		@Override
		public ContainedEntity newContained(int id) {
			ContainedEntity entity = new ContainedEntity();
			entity.setId( id );
			return entity;
		}

		@Override
		public void setIndexedField(ContainedEntity containedEntity, String value) {
			containedEntity.setIndexedField( value );
		}

		@Override
		public void setChild(ContainingEntity parent, ContainingEntity child) {
			parent.setChild( child );
		}

		@Override
		public void setParent(ContainingEntity child, ContainingEntity parent) {
			child.setParent( parent );

		}

		@Override
		public Map<ContainedEntity, String> newContainedAssociation(Map<ContainedEntity, String> original) {
			return new LinkedHashMap<>( original );
		}

		@Override
		public void addContained(Map<ContainedEntity, String> containedEntities, ContainedEntity containedEntity) {
			containedEntities.put( containedEntity, containedEntity.getIndexedField() + "_value" );
		}

		@Override
		public void removeContained(Map<ContainedEntity, String> containedEntities, ContainedEntity containedEntity) {
			containedEntities.remove( containedEntity );
		}

		@Override
		public void addContaining(List<ContainingEntity> containingEntities, ContainingEntity containingEntity) {
			containingEntities.add( containingEntity );
		}

		@Override
		public void removeContaining(List<ContainingEntity> containingEntities, ContainingEntity containingEntity) {
			containingEntities.remove( containingEntity );
		}

		@Override
		public Map<ContainedEntity, String> getContainedIndexedEmbedded(ContainingEntity containingEntity) {
			return containingEntity.getContainedIndexedEmbedded();
		}

		@Override
		public void setContainedIndexedEmbedded(ContainingEntity containingEntity,
				Map<ContainedEntity, String> containedEntities) {
			containingEntity.setContainedIndexedEmbedded( containedEntities );
		}

		@Override
		public List<ContainingEntity> getContainingAsIndexedEmbedded(ContainedEntity containedEntity) {
			return containedEntity.getContainingAsIndexedEmbedded();
		}

		@Override
		public Map<ContainedEntity, String> getContainedNonIndexedEmbedded(ContainingEntity containingEntity) {
			return containingEntity.getContainedNonIndexedEmbedded();
		}

		@Override
		public void setContainedNonIndexedEmbedded(ContainingEntity containingEntity,
				Map<ContainedEntity, String> containedEntities) {
			containingEntity.setContainedNonIndexedEmbedded( containedEntities );
		}

		@Override
		public List<ContainingEntity> getContainingAsNonIndexedEmbedded(ContainedEntity containedEntity) {
			return containedEntity.getContainingAsNonIndexedEmbedded();
		}
	}

	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		private Integer id;

		@OneToOne
		private ContainingEntity parent;

		@OneToOne(mappedBy = "parent")
		@IndexedEmbedded(includePaths = {
				"containedIndexedEmbedded.indexedField"
		})
		private ContainingEntity child;

		@ElementCollection
		@JoinTable(
				name = "indexed_containedIndexedEmbedded",
				joinColumns = @JoinColumn(name = "mapHolder")
		)
		@MapKeyJoinColumn(name = "key")
		@Column(name = "value")
		@OrderBy("key asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = "indexedField",
				extractors = @ContainerValueExtractorBeanReference( type = MapKeyExtractor.class )
		)
		private Map<ContainedEntity, String> containedIndexedEmbedded = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "indexed_containedNonIndexedEmbedded",
				joinColumns = @JoinColumn(name = "mapHolder")
		)
		@MapKeyJoinColumn(name = "key")
		@Column(name = "value")
		@OrderBy("key asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<ContainedEntity, String> containedNonIndexedEmbedded = new LinkedHashMap<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getParent() {
			return parent;
		}

		public void setParent(ContainingEntity parent) {
			this.parent = parent;
		}

		public ContainingEntity getChild() {
			return child;
		}

		public void setChild(ContainingEntity child) {
			this.child = child;
		}

		public Map<ContainedEntity, String> getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(Map<ContainedEntity, String> containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}

		public Map<ContainedEntity, String> getContainedNonIndexedEmbedded() {
			return containedNonIndexedEmbedded;
		}

		public void setContainedNonIndexedEmbedded(
				Map<ContainedEntity, String> containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity extends ContainingEntity {

		static final String INDEX = "IndexedEntity";

	}

	@Entity(name = "contained")
	public static class ContainedEntity {

		@Id
		private Integer id;

		/*
		 * No mappedBy here. The inverse side of associations modeled by a Map key cannot use mappedBy.
		 * If they do, Hibernate assumes that map *values* are the opposite side of the association,
		 * and ends up adding all kind of wrong foreign keys.
		 */
		@ManyToMany
		@JoinTable(name = "contained_mapHolder")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @PropertyValue(
						propertyName = "containedIndexedEmbedded",
						extractors = @ContainerValueExtractorBeanReference(type = MapKeyExtractor.class)
				)
		)
		private List<ContainingEntity> containingAsIndexedEmbedded = new ArrayList<>();

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@JoinTable(name = "contained_nonIndexedMapHolder")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsNonIndexedEmbedded = new ArrayList<>();

		@Basic
		@Field
		private String indexedField;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ContainingEntity> getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public List<ContainingEntity> getContainingAsNonIndexedEmbedded() {
			return containingAsNonIndexedEmbedded;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}
	}

}
