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
import javax.persistence.Transient;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

/**
 * Test automatic indexing based on Hibernate ORM entity events
 * when a Map-keys association is involved.
 * <p>
 * See {@link AbstractAutomaticIndexingAssociationIT} for more details on how this test is designed.
 */
public class AutomaticIndexingMapKeysAssociationIT extends AbstractAutomaticIndexingMultiAssociationIT<
		AutomaticIndexingMapKeysAssociationIT.IndexedEntity,
		AutomaticIndexingMapKeysAssociationIT.ContainingEntity,
		AutomaticIndexingMapKeysAssociationIT.ContainedEntity,
		Map<AutomaticIndexingMapKeysAssociationIT.ContainedEntity, String>,
		List<AutomaticIndexingMapKeysAssociationIT.ContainingEntity>
		> {

	public AutomaticIndexingMapKeysAssociationIT() {
		super( new MapKeysAssociationModelPrimitives() );
	}

	private static class MapKeysAssociationModelPrimitives
			implements MultiAssociationModelPrimitives<IndexedEntity, ContainingEntity, ContainedEntity,
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
		public void clearContained(Map<ContainedEntity, String> containedEntities) {
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

		@Override
		public Map<ContainedEntity, String> getContainedIndexedEmbeddedNoReindexOnUpdate(ContainingEntity containingEntity) {
			return containingEntity.getContainedIndexedEmbeddedNoReindexOnUpdate();
		}

		@Override
		public void setContainedIndexedEmbeddedNoReindexOnUpdate(ContainingEntity containingEntity,
				Map<ContainedEntity, String> containedEntities) {
			containingEntity.setContainedIndexedEmbeddedNoReindexOnUpdate( containedEntities );
		}

		@Override
		public List<ContainingEntity> getContainingAsIndexedEmbeddedNoReindexOnUpdate(ContainedEntity containedEntity) {
			return containedEntity.getContainingAsIndexedEmbeddedNoReindexOnUpdate();
		}

		@Override
		public Map<ContainedEntity, String> getContainedUsedInCrossEntityDerivedProperty(
				ContainingEntity containingEntity) {
			return containingEntity.getContainedUsedInCrossEntityDerivedProperty();
		}

		@Override
		public List<ContainingEntity> getContainingAsUsedInCrossEntityDerivedProperty(ContainedEntity containedEntity) {
			return containedEntity.getContainingAsUsedInCrossEntityDerivedProperty();
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

		@ElementCollection
		@JoinTable(
				name = "indexed_containedIndexedEmbedded",
				joinColumns = @JoinColumn(name = "mapHolder")
		)
		@MapKeyJoinColumn(name = "map_key")
		@Column(name = "value")
		@OrderBy("map_key asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" },
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		private Map<ContainedEntity, String> containedIndexedEmbedded = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "indexed_containedNonIndexedEmbedded",
				joinColumns = @JoinColumn(name = "mapHolder")
		)
		@MapKeyJoinColumn(name = "map_key")
		@Column(name = "value")
		@OrderBy("map_key asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<ContainedEntity, String> containedNonIndexedEmbedded = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "indexed_containedIndexedEmbeddedNoReindexOnUpdate",
				joinColumns = @JoinColumn(name = "mapHolder")
		)
		@MapKeyJoinColumn(name = "map_key")
		@Column(name = "value")
		@OrderBy("map_key asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" },
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		@IndexingDependency(
				reindexOnUpdate = ReindexOnUpdate.NO,
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		private Map<ContainedEntity, String> containedIndexedEmbeddedNoReindexOnUpdate = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "indexed_containedUsedInCrossEntityDerivedProperty",
				joinColumns = @JoinColumn(name = "mapHolder")
		)
		@MapKeyJoinColumn(name = "map_key")
		@Column(name = "value")
		@OrderBy("map_key asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<ContainedEntity, String> containedUsedInCrossEntityDerivedProperty = new LinkedHashMap<>();

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

		public void setContainedNonIndexedEmbedded(Map<ContainedEntity, String> containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}

		public Map<ContainedEntity, String> getContainedIndexedEmbeddedNoReindexOnUpdate() {
			return containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedNoReindexOnUpdate(
				Map<ContainedEntity, String> containedIndexedEmbeddedNoReindexOnUpdate) {
			this.containedIndexedEmbeddedNoReindexOnUpdate = containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public Map<ContainedEntity, String> getContainedUsedInCrossEntityDerivedProperty() {
			return containedUsedInCrossEntityDerivedProperty;
		}

		@Transient
		@GenericField
		@IndexingDependency(derivedFrom = {
				@ObjectPath({
						@PropertyValue(
								propertyName = "containedUsedInCrossEntityDerivedProperty",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField1")
				}),
				@ObjectPath({
						@PropertyValue(
								propertyName = "containedUsedInCrossEntityDerivedProperty",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField2")
				})
		})
		public Optional<String> getCrossEntityDerivedField() {
			return computeDerived(
					containedUsedInCrossEntityDerivedProperty.keySet().stream().flatMap( c -> Stream.of(
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

		/*
		 * No mappedBy here. The inverse side of associations modeled by a Map key cannot use mappedBy.
		 * If they do, Hibernate assumes that map *values* are the opposite side of the association,
		 * and ends up adding all kind of wrong foreign keys.
		 */
		@ManyToMany
		@JoinTable(name = "contained_mapHolder")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
						@PropertyValue(
								propertyName = "containedIndexedEmbedded",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						)
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

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@JoinTable(name = "contained_indexedNoReindexOnUpdateMapHolder")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
						@PropertyValue(
								propertyName = "containedIndexedEmbeddedNoReindexOnUpdate",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						)
				)
		)
		private List<ContainingEntity> containingAsIndexedEmbeddedNoReindexOnUpdate = new ArrayList<>();

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
						@PropertyValue(
								propertyName = "containedUsedInCrossEntityDerivedProperty",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						)
				)
		)
		private List<ContainingEntity> containingAsUsedInCrossEntityDerivedProperty = new ArrayList<>();

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

		public List<ContainingEntity> getContainingAsUsedInCrossEntityDerivedProperty() {
			return containingAsUsedInCrossEntityDerivedProperty;
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
