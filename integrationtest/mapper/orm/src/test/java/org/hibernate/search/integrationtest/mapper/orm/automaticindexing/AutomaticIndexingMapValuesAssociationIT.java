/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Transient;

import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

/**
 * Test automatic indexing based on Hibernate ORM entity events
 * when a Map-values association is involved.
 * <p>
 * See {@link AbstractAutomaticIndexingAssociationIT} for more details on how this test is designed.
 */
public class AutomaticIndexingMapValuesAssociationIT extends AbstractAutomaticIndexingMultiAssociationIT<
		AutomaticIndexingMapValuesAssociationIT.IndexedEntity,
		AutomaticIndexingMapValuesAssociationIT.ContainingEntity,
		AutomaticIndexingMapValuesAssociationIT.ContainedEntity,
		Map<String, AutomaticIndexingMapValuesAssociationIT.ContainedEntity>,
		List<AutomaticIndexingMapValuesAssociationIT.ContainingEntity>
		> {

	public AutomaticIndexingMapValuesAssociationIT() {
		super( new MapValuesAssociationModelPrimitives() );
	}

	private static class MapValuesAssociationModelPrimitives
			implements MultiAssociationModelPrimitives<IndexedEntity, ContainingEntity, ContainedEntity,
						Map<String, ContainedEntity>, List<ContainingEntity>> {

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
		public void setChild(ContainingEntity parent, ContainingEntity child) {
			parent.setChild( child );
		}

		@Override
		public void setParent(ContainingEntity child, ContainingEntity parent) {
			child.setParent( parent );
		}

		@Override
		public Map<String, ContainedEntity> newContainedAssociation(Map<String, ContainedEntity> original) {
			return new LinkedHashMap<>( original );
		}

		@Override
		public void addContained(Map<String, ContainedEntity> containedEntities, ContainedEntity containedEntity) {
			containedEntities.put( containedEntity.getIndexedField() + "_key", containedEntity );
		}

		@Override
		public void removeContained(Map<String, ContainedEntity> containedEntities, ContainedEntity containedEntity) {
			containedEntities.remove( containedEntity.getIndexedField() + "_key" );
		}

		@Override
		public void clearContained(Map<String, ContainedEntity> containedEntities) {
			containedEntities.clear();
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
		public void clearContaining(List<ContainingEntity> containingEntities) {
			containingEntities.clear();
		}

		@Override
		public Map<String, ContainedEntity> getContainedIndexedEmbedded(ContainingEntity containingEntity) {
			return containingEntity.getContainedIndexedEmbedded();
		}

		@Override
		public void setContainedIndexedEmbedded(ContainingEntity containingEntity,
				Map<String, ContainedEntity> containedEntities) {
			containingEntity.setContainedIndexedEmbedded( containedEntities );
		}

		@Override
		public List<ContainingEntity> getContainingAsIndexedEmbedded(ContainedEntity containedEntity) {
			return containedEntity.getContainingAsIndexedEmbedded();
		}

		@Override
		public Map<String, ContainedEntity> getContainedNonIndexedEmbedded(ContainingEntity containingEntity) {
			return containingEntity.getContainedNonIndexedEmbedded();
		}

		@Override
		public void setContainedNonIndexedEmbedded(ContainingEntity containingEntity,
				Map<String, ContainedEntity> containedEntities) {
			containingEntity.setContainedNonIndexedEmbedded( containedEntities );
		}

		@Override
		public List<ContainingEntity> getContainingAsNonIndexedEmbedded(ContainedEntity containedEntity) {
			return containedEntity.getContainingAsNonIndexedEmbedded();
		}

		@Override
		public Map<String, ContainedEntity> getContainedIndexedEmbeddedNoReindexOnUpdate(ContainingEntity containingEntity) {
			return containingEntity.getContainedIndexedEmbeddedNoReindexOnUpdate();
		}

		@Override
		public void setContainedIndexedEmbeddedNoReindexOnUpdate(ContainingEntity containingEntity,
				Map<String, ContainedEntity> containedEntities) {
			containingEntity.setContainedIndexedEmbeddedNoReindexOnUpdate( containedEntities );
		}

		@Override
		public List<ContainingEntity> getContainingAsIndexedEmbeddedNoReindexOnUpdate(ContainedEntity containedEntity) {
			return containedEntity.getContainingAsIndexedEmbeddedNoReindexOnUpdate();
		}

		@Override
		public void setIndexedField(ContainedEntity containedEntity, String value) {
			containedEntity.setIndexedField( value );
		}

		@Override
		public void setNonIndexedField(ContainedEntity containedEntity, String value) {
			containedEntity.setNonIndexedField( value );
		}

		@Override
		public List<String> getIndexedElementCollectionField(ContainedEntity containedEntity) {
			return containedEntity.getIndexedElementCollectionField();
		}

		@Override
		public void setIndexedElementCollectionField(ContainedEntity containedEntity, List<String> value) {
			containedEntity.setIndexedElementCollectionField( value );
		}

		@Override
		public List<String> getNonIndexedElementCollectionField(ContainedEntity containedEntity) {
			return containedEntity.getNonIndexedElementCollectionField();
		}

		@Override
		public void setNonIndexedElementCollectionField(ContainedEntity containedEntity, List<String> value) {
			containedEntity.setNonIndexedElementCollectionField( value );
		}

		@Override
		public void setFieldUsedInContainedDerivedField1(ContainedEntity containedEntity, String value) {
			containedEntity.setFieldUsedInContainedDerivedField1( value );
		}

		@Override
		public void setFieldUsedInContainedDerivedField2(ContainedEntity containedEntity, String value) {
			containedEntity.setFieldUsedInContainedDerivedField2( value );
		}

		@Override
		public void setFieldUsedInCrossEntityDerivedField1(ContainedEntity containedEntity, String value) {
			containedEntity.setFieldUsedInCrossEntityDerivedField1( value );
		}

		@Override
		public void setFieldUsedInCrossEntityDerivedField2(ContainedEntity containedEntity, String value) {
			containedEntity.setFieldUsedInCrossEntityDerivedField2( value );
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
				"containedIndexedEmbedded.indexedField",
				"containedIndexedEmbedded.indexedElementCollectionField",
				"containedIndexedEmbedded.containedDerivedField",
				"containedIndexedEmbeddedNoReindexOnUpdate.indexedField",
				"containedIndexedEmbeddedNoReindexOnUpdate.indexedElementCollectionField",
				"containedIndexedEmbeddedNoReindexOnUpdate.containedDerivedField",
				"crossEntityDerivedField"
		})
		private ContainingEntity child;

		@ManyToMany
		@JoinTable(
				name = "indexed_containedIndexedEmbedded",
				joinColumns = @JoinColumn(name = "mapHolder"),
				inverseJoinColumns = @JoinColumn(name = "value")
		)
		@MapKeyColumn(name = "map_key")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@OrderBy("id asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<String, ContainedEntity> containedIndexedEmbedded = new LinkedHashMap<>();

		@ManyToMany
		@JoinTable(
				name = "indexed_containedNonIndexedEmbedded",
				joinColumns = @JoinColumn(name = "mapHolder"),
				inverseJoinColumns = @JoinColumn(name = "value")
		)
		@MapKeyColumn(name = "map_key")
		@OrderBy("id asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<String, ContainedEntity> containedNonIndexedEmbedded = new LinkedHashMap<>();

		@ManyToMany
		@JoinTable(
				name = "indexed_containedIndexedEmbeddedNoReindexOnUpdate",
				joinColumns = @JoinColumn(name = "mapHolder"),
				inverseJoinColumns = @JoinColumn(name = "value")
		)
		@MapKeyColumn(name = "map_key")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		@OrderBy("id asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<String, ContainedEntity> containedIndexedEmbeddedNoReindexOnUpdate = new LinkedHashMap<>();

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

		public Map<String, ContainedEntity> getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(Map<String, ContainedEntity> containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}

		public Map<String, ContainedEntity> getContainedNonIndexedEmbedded() {
			return containedNonIndexedEmbedded;
		}

		public void setContainedNonIndexedEmbedded(Map<String, ContainedEntity> containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}

		public Map<String, ContainedEntity> getContainedIndexedEmbeddedNoReindexOnUpdate() {
			return containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedNoReindexOnUpdate(
				Map<String, ContainedEntity> containedIndexedEmbeddedNoReindexOnUpdate) {
			this.containedIndexedEmbeddedNoReindexOnUpdate = containedIndexedEmbeddedNoReindexOnUpdate;
		}

		@Transient
		@GenericField
		@IndexingDependency(derivedFrom = {
				@ObjectPath({
						@PropertyValue(propertyName = "containedIndexedEmbedded"),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField1")
				}),
				@ObjectPath({
						@PropertyValue(propertyName = "containedIndexedEmbedded"),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField2")
				})
		})
		public Optional<String> getCrossEntityDerivedField() {
			return computeDerived(
					containedIndexedEmbedded.values().stream().flatMap( c -> Stream.of(
							c.getFieldUsedInCrossEntityDerivedField1(),
							c.getFieldUsedInCrossEntityDerivedField2()
					) )
			);
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

		@ManyToMany(mappedBy = "containedIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsIndexedEmbedded = new ArrayList<>();

		@ManyToMany(mappedBy = "containedNonIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsNonIndexedEmbedded = new ArrayList<>();

		@ManyToMany(mappedBy = "containedIndexedEmbeddedNoReindexOnUpdate")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsIndexedEmbeddedNoReindexOnUpdate = new ArrayList<>();

		@Basic
		@GenericField
		private String indexedField;

		@ElementCollection
		@GenericField
		private List<String> indexedElementCollectionField = new ArrayList<>();

		@Basic
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private String nonIndexedField;

		@ElementCollection
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private List<String> nonIndexedElementCollectionField = new ArrayList<>();

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		private String fieldUsedInContainedDerivedField1;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		private String fieldUsedInContainedDerivedField2;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		private String fieldUsedInCrossEntityDerivedField1;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		private String fieldUsedInCrossEntityDerivedField2;

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

		public List<ContainingEntity> getContainingAsIndexedEmbeddedNoReindexOnUpdate() {
			return containingAsIndexedEmbeddedNoReindexOnUpdate;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}

		public List<String> getIndexedElementCollectionField() {
			return indexedElementCollectionField;
		}

		public void setIndexedElementCollectionField(List<String> indexedElementCollectionField) {
			this.indexedElementCollectionField = indexedElementCollectionField;
		}

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
		}

		public List<String> getNonIndexedElementCollectionField() {
			return nonIndexedElementCollectionField;
		}

		public void setNonIndexedElementCollectionField(List<String> nonIndexedElementCollectionField) {
			this.nonIndexedElementCollectionField = nonIndexedElementCollectionField;
		}

		public String getFieldUsedInContainedDerivedField1() {
			return fieldUsedInContainedDerivedField1;
		}

		public void setFieldUsedInContainedDerivedField1(String fieldUsedInContainedDerivedField1) {
			this.fieldUsedInContainedDerivedField1 = fieldUsedInContainedDerivedField1;
		}

		public String getFieldUsedInContainedDerivedField2() {
			return fieldUsedInContainedDerivedField2;
		}

		public void setFieldUsedInContainedDerivedField2(String fieldUsedInContainedDerivedField2) {
			this.fieldUsedInContainedDerivedField2 = fieldUsedInContainedDerivedField2;
		}

		public String getFieldUsedInCrossEntityDerivedField1() {
			return fieldUsedInCrossEntityDerivedField1;
		}

		public void setFieldUsedInCrossEntityDerivedField1(String fieldUsedInCrossEntityDerivedField1) {
			this.fieldUsedInCrossEntityDerivedField1 = fieldUsedInCrossEntityDerivedField1;
		}

		public String getFieldUsedInCrossEntityDerivedField2() {
			return fieldUsedInCrossEntityDerivedField2;
		}

		public void setFieldUsedInCrossEntityDerivedField2(String fieldUsedInCrossEntityDerivedField2) {
			this.fieldUsedInCrossEntityDerivedField2 = fieldUsedInCrossEntityDerivedField2;
		}

		@Transient
		@GenericField
		@IndexingDependency(derivedFrom = {
				@ObjectPath(@PropertyValue(propertyName = "fieldUsedInContainedDerivedField1")),
				@ObjectPath(@PropertyValue(propertyName = "fieldUsedInContainedDerivedField2"))
		})
		public Optional<String> getContainedDerivedField() {
			return computeDerived( Stream.of( fieldUsedInContainedDerivedField1, fieldUsedInContainedDerivedField2 ) );
		}
	}

}
