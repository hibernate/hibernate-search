/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.manytoone;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.AbstractAutomaticIndexingAssociationBaseIT;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.ContainerPrimitives;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.MultiValuedPropertyAccessor;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.PropertyAccessor;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.SingleValuedPropertyAccessor;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

/**
 * Test automatic indexing caused by multi-valued association updates
 * or by updates of associated (contained) entities,
 * with a {@code @ManyToOne} association owned by the containing side.
 */
public class AutomaticIndexingManyToOneBaseIT
		extends AbstractAutomaticIndexingAssociationBaseIT<
						AutomaticIndexingManyToOneBaseIT.IndexedEntity,
						AutomaticIndexingManyToOneBaseIT.ContainingEntity,
						AutomaticIndexingManyToOneBaseIT.ContainedEntity
				> {

	public AutomaticIndexingManyToOneBaseIT() {
		super( new ModelPrimitivesImpl() );
	}

	private static class ModelPrimitivesImpl
			implements ModelPrimitives<IndexedEntity, ContainingEntity, ContainedEntity> {

		@Override
		public String getIndexName() {
			return IndexedEntity.INDEX;
		}

		@Override
		public boolean isMultiValuedAssociation() {
			return false;
		}

		@Override
		public boolean isAssociationOwnedByContainedSide() {
			return false;
		}

		@Override
		public boolean isAssociationLazyOnContainingSide() {
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
		public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbedded() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setContainedIndexedEmbedded,
					ContainingEntity::getContainedIndexedEmbedded );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbedded() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsIndexedEmbedded );
		}

		@Override
		public PropertyAccessor<ContainingEntity, ContainedEntity> containedNonIndexedEmbedded() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setContainedNonIndexedEmbedded,
					ContainingEntity::getContainedNonIndexedEmbedded );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsNonIndexedEmbedded() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsNonIndexedEmbedded );
		}

		@Override
		public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedShallowReindexOnUpdate() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setContainedIndexedEmbeddedShallowReindexOnUpdate,
					ContainingEntity::getContainedIndexedEmbeddedShallowReindexOnUpdate );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbeddedShallowReindexOnUpdate() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsIndexedEmbeddedShallowReindexOnUpdate );
		}

		@Override
		public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedNoReindexOnUpdate() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setContainedIndexedEmbeddedNoReindexOnUpdate,
					ContainingEntity::getContainedIndexedEmbeddedNoReindexOnUpdate );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsIndexedEmbeddedNoReindexOnUpdate() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsIndexedEmbeddedNoReindexOnUpdate );
		}

		@Override
		public PropertyAccessor<ContainingEntity, ContainedEntity> containedUsedInCrossEntityDerivedProperty() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setContainedUsedInCrossEntityDerivedProperty,
					ContainingEntity::getContainedUsedInCrossEntityDerivedProperty );
		}

		@Override
		public MultiValuedPropertyAccessor<ContainedEntity, ContainingEntity, List<ContainingEntity>> containingAsUsedInCrossEntityDerivedProperty() {
			return new MultiValuedPropertyAccessor<>( ContainerPrimitives.collection(),
					ContainedEntity::getContainingAsUsedInCrossEntityDerivedProperty );
		}

		@Override
		public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedWithCast() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setContainedIndexedEmbeddedWithCast );
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
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

		@ManyToOne
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		private ContainedEntity containedIndexedEmbedded;

		@ManyToOne
		private ContainedEntity containedNonIndexedEmbedded;

		@ManyToOne
		@JoinColumn(name = "CIndexedEmbeddedSROU")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
		private ContainedEntity containedIndexedEmbeddedShallowReindexOnUpdate;

		@ManyToOne
		@JoinColumn(name = "CIndexedEmbeddedNROU")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		private ContainedEntity containedIndexedEmbeddedNoReindexOnUpdate;

		@ManyToOne
		@JoinColumn(name = "CCrossEntityDerived")
		private ContainedEntity containedUsedInCrossEntityDerivedProperty;

		@ManyToOne(targetEntity = ContainedEntity.class)
		@JoinColumn(name = "CIndexedEmbeddedCast")
		@IndexedEmbedded(includePaths = { "indexedField" }, targetType = ContainedEntity.class)
		private Object containedIndexedEmbeddedWithCast;

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

		public ContainedEntity getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(ContainedEntity containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}

		public ContainedEntity getContainedNonIndexedEmbedded() {
			return containedNonIndexedEmbedded;
		}

		public void setContainedNonIndexedEmbedded(ContainedEntity containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}

		public ContainedEntity getContainedIndexedEmbeddedShallowReindexOnUpdate() {
			return containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedShallowReindexOnUpdate(
				ContainedEntity containedIndexedEmbeddedShallowReindexOnUpdate) {
			this.containedIndexedEmbeddedShallowReindexOnUpdate = containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public ContainedEntity getContainedIndexedEmbeddedNoReindexOnUpdate() {
			return containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedNoReindexOnUpdate(
				ContainedEntity containedIndexedEmbeddedNoReindexOnUpdate) {
			this.containedIndexedEmbeddedNoReindexOnUpdate = containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public ContainedEntity getContainedUsedInCrossEntityDerivedProperty() {
			return containedUsedInCrossEntityDerivedProperty;
		}

		public void setContainedUsedInCrossEntityDerivedProperty(
				ContainedEntity containedUsedInCrossEntityDerivedProperty) {
			this.containedUsedInCrossEntityDerivedProperty = containedUsedInCrossEntityDerivedProperty;
		}

		public Object getContainedIndexedEmbeddedWithCast() {
			return containedIndexedEmbeddedWithCast;
		}

		public void setContainedIndexedEmbeddedWithCast(Object containedIndexedEmbeddedWithCast) {
			this.containedIndexedEmbeddedWithCast = containedIndexedEmbeddedWithCast;
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
			return containedUsedInCrossEntityDerivedProperty == null
					? Optional.empty()
					: computeDerived( Stream.of(
							containedUsedInCrossEntityDerivedProperty.getFieldUsedInCrossEntityDerivedField1(),
							containedUsedInCrossEntityDerivedProperty.getFieldUsedInCrossEntityDerivedField2()
					) );
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

		@OneToMany(mappedBy = "containedIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsIndexedEmbedded = new ArrayList<>();

		@OneToMany(mappedBy = "containedNonIndexedEmbedded")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsNonIndexedEmbedded = new ArrayList<>();

		@OneToMany(mappedBy = "containedIndexedEmbeddedShallowReindexOnUpdate")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsIndexedEmbeddedShallowReindexOnUpdate = new ArrayList<>();

		@OneToMany(mappedBy = "containedIndexedEmbeddedNoReindexOnUpdate")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsIndexedEmbeddedNoReindexOnUpdate = new ArrayList<>();

		@OneToMany(mappedBy = "containedUsedInCrossEntityDerivedProperty")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsUsedInCrossEntityDerivedProperty = new ArrayList<>();

		@OneToMany(mappedBy = "containedIndexedEmbeddedWithCast", targetEntity = ContainingEntity.class)
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<Object> containingAsIndexedEmbeddedWithCast = new ArrayList<>();

		@Basic
		@GenericField
		private String indexedField;

		@ElementCollection
		@OrderColumn(name = "idx")
		@CollectionTable(name = "indexedECF")
		@GenericField
		private List<String> indexedElementCollectionField = new ArrayList<>();

		@Basic
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private String nonIndexedField;

		@ElementCollection
		@OrderColumn(name = "idx")
		@CollectionTable(name = "nonIndexedECF")
		@Column(name = "nonIndexedECF")
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
