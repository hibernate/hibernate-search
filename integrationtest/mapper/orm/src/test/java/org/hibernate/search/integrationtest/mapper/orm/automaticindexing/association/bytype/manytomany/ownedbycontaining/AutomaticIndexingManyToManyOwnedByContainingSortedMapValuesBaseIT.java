/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.manytomany.ownedbycontaining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;

import org.hibernate.annotations.SortNatural;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.AbstractAutomaticIndexingMultiValuedAssociationBaseIT;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.ContainerPrimitives;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.MultiValuedPropertyAccessor;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.SingleValuedPropertyAccessor;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

/**
 * Test automatic indexing caused by multi-valued association updates
 * or by updates of associated (contained) entities,
 * with a {@code @ManyToMany SortedMap} association owned by the containing side
 * where associated entities are map values.
 */
@TestForIssue(jiraKey = "HSEARCH-2490")
public class AutomaticIndexingManyToManyOwnedByContainingSortedMapValuesBaseIT
		extends AbstractAutomaticIndexingMultiValuedAssociationBaseIT<
						AutomaticIndexingManyToManyOwnedByContainingSortedMapValuesBaseIT.IndexedEntity,
						AutomaticIndexingManyToManyOwnedByContainingSortedMapValuesBaseIT.ContainingEntity,
						AutomaticIndexingManyToManyOwnedByContainingSortedMapValuesBaseIT.ContainedEntity,
						SortedMap<String, AutomaticIndexingManyToManyOwnedByContainingSortedMapValuesBaseIT.ContainedEntity>
		> {

	public AutomaticIndexingManyToManyOwnedByContainingSortedMapValuesBaseIT() {
		super( new ModelPrimitivesImpl() );
	}

	private static class ModelPrimitivesImpl
			implements MultiValuedModelPrimitives<IndexedEntity, ContainingEntity, ContainedEntity,
											SortedMap<String, ContainedEntity>> {

		private final ContainerPrimitives<SortedMap<String, ContainedEntity>, ContainedEntity> MAP_VALUES_PRIMITIVES =
				ContainerPrimitives.mapValues( containedEntity -> containedEntity.getIndexedField() + "_value" );

		@Override
		public String getIndexName() {
			return IndexedEntity.INDEX;
		}

		@Override
		public boolean isAssociationOwnedByContainedSide() {
			return false;
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
		public SingleValuedPropertyAccessor<ContainingEntity, String> containingEntityNonIndexedField() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setNonIndexedField );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainingEntity, ContainingEntity> child() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setChild );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainingEntity, ContainingEntity> parent() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setParent );
		}

		@Override
		public SortedMap<String, ContainedEntity> newContainedAssociation(SortedMap<String, ContainedEntity> original) {
			return new TreeMap<>( original );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, SortedMap<String, ContainedEntity>> containedIndexedEmbedded() {
			return new MultiValuedPropertyAccessor<>(
					MAP_VALUES_PRIMITIVES,
					ContainingEntity::getContainedIndexedEmbedded, ContainingEntity::setContainedIndexedEmbedded );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbedded() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsIndexedEmbedded );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, SortedMap<String, ContainedEntity>> containedNonIndexedEmbedded() {
			return new MultiValuedPropertyAccessor<>(
					MAP_VALUES_PRIMITIVES,
					ContainingEntity::getContainedNonIndexedEmbedded, ContainingEntity::setContainedNonIndexedEmbedded );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsNonIndexedEmbedded() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsNonIndexedEmbedded );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, SortedMap<String, ContainedEntity>> containedIndexedEmbeddedShallowReindexOnUpdate() {
			return new MultiValuedPropertyAccessor<>(
					MAP_VALUES_PRIMITIVES,
					ContainingEntity::getContainedIndexedEmbeddedShallowReindexOnUpdate,
					ContainingEntity::setContainedIndexedEmbeddedShallowReindexOnUpdate );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbeddedShallowReindexOnUpdate() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsIndexedEmbeddedShallowReindexOnUpdate );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, SortedMap<String, ContainedEntity>> containedIndexedEmbeddedNoReindexOnUpdate() {
			return new MultiValuedPropertyAccessor<>(
					MAP_VALUES_PRIMITIVES,
					ContainingEntity::getContainedIndexedEmbeddedNoReindexOnUpdate,
					ContainingEntity::setContainedIndexedEmbeddedNoReindexOnUpdate );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbeddedNoReindexOnUpdate() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsIndexedEmbeddedNoReindexOnUpdate );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, SortedMap<String, ContainedEntity>> containedUsedInCrossEntityDerivedProperty() {
			return new MultiValuedPropertyAccessor<>(
					MAP_VALUES_PRIMITIVES,
					ContainingEntity::getContainedUsedInCrossEntityDerivedProperty );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsUsedInCrossEntityDerivedProperty() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsUsedInCrossEntityDerivedProperty );
		}

		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public MultiValuedPropertyAccessor<ContainingEntity, ContainedEntity, SortedMap<String, ContainedEntity>> containedIndexedEmbeddedWithCast() {
			return new MultiValuedPropertyAccessor<>(
					MAP_VALUES_PRIMITIVES,
					root -> (SortedMap) root.getContainedIndexedEmbeddedWithCast() );
		}

		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbeddedWithCast() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					root -> (List) root.getContainingAsIndexedEmbeddedWithCast() );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainedEntity, String> indexedField() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setIndexedField );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainedEntity, String> nonIndexedField() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setNonIndexedField );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, String, List<String>> indexedElementCollectionField() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getIndexedElementCollectionField,
					ContainedEntity::setIndexedElementCollectionField );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, String, List<String>> nonIndexedElementCollectionField() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getNonIndexedElementCollectionField,
					ContainedEntity::setNonIndexedElementCollectionField );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainedEntity, String> fieldUsedInContainedDerivedField1() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setFieldUsedInContainedDerivedField1 );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainedEntity, String> fieldUsedInContainedDerivedField2() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setFieldUsedInContainedDerivedField2 );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainedEntity, String> fieldUsedInCrossEntityDerivedField1() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setFieldUsedInCrossEntityDerivedField1 );
		}

		@Override
		public SingleValuedPropertyAccessor<ContainedEntity, String> fieldUsedInCrossEntityDerivedField2() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setFieldUsedInCrossEntityDerivedField2 );
		}
	}

	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		private Integer id;

		private String nonIndexedField;

		@OneToOne
		private ContainingEntity parent;

		@OneToOne(mappedBy = "parent")
		@IndexedEmbedded(includePaths = {
				"containedIndexedEmbedded.indexedField",
				"containedIndexedEmbedded.indexedElementCollectionField",
				"containedIndexedEmbedded.containedDerivedField",
				"containedIndexedEmbeddedShallowReindexOnUpdate.indexedField",
				"containedIndexedEmbeddedShallowReindexOnUpdate.indexedElementCollectionField",
				"containedIndexedEmbeddedShallowReindexOnUpdate.containedDerivedField",
				"containedIndexedEmbeddedNoReindexOnUpdate.indexedField",
				"containedIndexedEmbeddedNoReindexOnUpdate.indexedElementCollectionField",
				"containedIndexedEmbeddedNoReindexOnUpdate.containedDerivedField",
				"containedIndexedEmbeddedWithCast.indexedField",
				"crossEntityDerivedField"
		})
		private ContainingEntity child;

		@ManyToMany
		@JoinTable(
				name = "i_containedIndexedEmbedded",
				joinColumns = @JoinColumn(name = "containing"),
				inverseJoinColumns = @JoinColumn(name = "contained")
		)
		@MapKeyColumn(name = "map_key")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@SortNatural
		private SortedMap<String, ContainedEntity> containedIndexedEmbedded = new TreeMap<>();

		@ManyToMany
		@JoinTable(
				name = "i_containedNonIndexedEmbedded",
				joinColumns = @JoinColumn(name = "containing"),
				inverseJoinColumns = @JoinColumn(name = "contained")
		)
		@MapKeyColumn(name = "map_key")
		@SortNatural
		private SortedMap<String, ContainedEntity> containedNonIndexedEmbedded = new TreeMap<>();

		@ManyToMany
		@JoinTable(
				name = "i_indexedEmbeddedShallow",
				joinColumns = @JoinColumn(name = "containing"),
				inverseJoinColumns = @JoinColumn(name = "contained")
		)
		@MapKeyColumn(name = "map_key")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
		@SortNatural
		private SortedMap<String, ContainedEntity> containedIndexedEmbeddedShallowReindexOnUpdate = new TreeMap<>();

		@ManyToMany
		@JoinTable(
				name = "i_indexedEmbeddedNoReindex",
				joinColumns = @JoinColumn(name = "containing"),
				inverseJoinColumns = @JoinColumn(name = "contained")
		)
		@MapKeyColumn(name = "map_key")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		@SortNatural
		private SortedMap<String, ContainedEntity> containedIndexedEmbeddedNoReindexOnUpdate = new TreeMap<>();

		@ManyToMany
		@JoinTable(
				name = "i_containedCrossEntityDP",
				joinColumns = @JoinColumn(name = "containing"),
				inverseJoinColumns = @JoinColumn(name = "contained")
		)
		@MapKeyColumn(name = "map_key")
		@SortNatural
		private SortedMap<String, ContainedEntity> containedUsedInCrossEntityDerivedProperty = new TreeMap<>();

		@ManyToMany(targetEntity = ContainedEntity.class)
		@JoinTable(
				name = "i_containedIndexedEmbeddedCast",
				joinColumns = @JoinColumn(name = "containing"),
				inverseJoinColumns = @JoinColumn(name = "contained")
		)
		@MapKeyColumn(name = "map_key")
		@IndexedEmbedded(includePaths = "indexedField", targetType = ContainedEntity.class)
		@SortNatural
		private SortedMap<String, Object> containedIndexedEmbeddedWithCast = new TreeMap<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
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

		public SortedMap<String, ContainedEntity> getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(SortedMap<String, ContainedEntity> containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}

		public SortedMap<String, ContainedEntity> getContainedNonIndexedEmbedded() {
			return containedNonIndexedEmbedded;
		}

		public void setContainedNonIndexedEmbedded(SortedMap<String, ContainedEntity> containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}

		public SortedMap<String, ContainedEntity> getContainedIndexedEmbeddedShallowReindexOnUpdate() {
			return containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedShallowReindexOnUpdate(
				SortedMap<String, ContainedEntity> containedIndexedEmbeddedShallowReindexOnUpdate) {
			this.containedIndexedEmbeddedShallowReindexOnUpdate = containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public SortedMap<String, ContainedEntity> getContainedIndexedEmbeddedNoReindexOnUpdate() {
			return containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedNoReindexOnUpdate(
				SortedMap<String, ContainedEntity> containedIndexedEmbeddedNoReindexOnUpdate) {
			this.containedIndexedEmbeddedNoReindexOnUpdate = containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public SortedMap<String, ContainedEntity> getContainedUsedInCrossEntityDerivedProperty() {
			return containedUsedInCrossEntityDerivedProperty;
		}

		public SortedMap<String, Object> getContainedIndexedEmbeddedWithCast() {
			return containedIndexedEmbeddedWithCast;
		}

		@Transient
		@GenericField
		@IndexingDependency(derivedFrom = {
				@ObjectPath({
						@PropertyValue(propertyName = "containedUsedInCrossEntityDerivedProperty"),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField1")
				}),
				@ObjectPath({
						@PropertyValue(propertyName = "containedUsedInCrossEntityDerivedProperty"),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField2")
				})
		})
		public Optional<String> getCrossEntityDerivedField() {
			return computeDerived(
					containedUsedInCrossEntityDerivedProperty.values().stream().flatMap( c -> Stream.of(
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

		@ManyToMany(mappedBy = "containedIndexedEmbeddedShallowReindexOnUpdate")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsIndexedEmbeddedShallowReindexOnUpdate = new ArrayList<>();

		@ManyToMany(mappedBy = "containedIndexedEmbeddedNoReindexOnUpdate")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsIndexedEmbeddedNoReindexOnUpdate = new ArrayList<>();

		@ManyToMany(mappedBy = "containedUsedInCrossEntityDerivedProperty")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsUsedInCrossEntityDerivedProperty = new ArrayList<>();

		@ManyToMany(mappedBy = "containedIndexedEmbeddedWithCast", targetEntity = ContainingEntity.class)
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<Object> containingAsIndexedEmbeddedWithCast = new ArrayList<>();

		@Basic
		@GenericField
		private String indexedField;

		@ElementCollection
		@OrderColumn(name = "idx")
		@CollectionTable(name = "contained_IElementCF")
		@GenericField
		private List<String> indexedElementCollectionField = new ArrayList<>();

		@Basic
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private String nonIndexedField;

		@ElementCollection
		@OrderColumn(name = "idx")
		@CollectionTable(name = "nonIndexedECF")
		@Column(name = "nonIndexed")
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private List<String> nonIndexedElementCollectionField = new ArrayList<>();

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUIContainedDF1")
		private String fieldUsedInContainedDerivedField1;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUIContainedDF2")
		private String fieldUsedInContainedDerivedField2;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUICrossEntityDF1")
		private String fieldUsedInCrossEntityDerivedField1;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUICrossEntityDF2")
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

		public List<ContainingEntity> getContainingAsIndexedEmbeddedShallowReindexOnUpdate() {
			return containingAsIndexedEmbeddedShallowReindexOnUpdate;
		}

		public List<ContainingEntity> getContainingAsIndexedEmbeddedNoReindexOnUpdate() {
			return containingAsIndexedEmbeddedNoReindexOnUpdate;
		}

		public List<ContainingEntity> getContainingAsUsedInCrossEntityDerivedProperty() {
			return containingAsUsedInCrossEntityDerivedProperty;
		}

		public List<Object> getContainingAsIndexedEmbeddedWithCast() {
			return containingAsIndexedEmbeddedWithCast;
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
