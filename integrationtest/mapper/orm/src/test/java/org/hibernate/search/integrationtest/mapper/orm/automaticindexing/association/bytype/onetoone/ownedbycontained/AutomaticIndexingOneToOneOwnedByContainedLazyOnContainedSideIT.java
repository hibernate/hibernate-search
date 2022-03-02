/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.onetoone.ownedbycontained;

import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
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
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.junit.runner.RunWith;

/**
 * Test automatic indexing caused by single-valued association updates
 * or by updates of associated (contained) entities,
 * with a {@code @OneToOne} association owned by the contained side,
 * and with lazy associations on the contained side.
 */
@RunWith(BytecodeEnhancerRunner.class) // So that we can have lazy *ToOne associations
@EnhancementOptions(lazyLoading = true)
@TestForIssue(jiraKey = "HSEARCH-4305")
public class AutomaticIndexingOneToOneOwnedByContainedLazyOnContainedSideIT
		extends AbstractAutomaticIndexingAssociationBaseIT<
						AutomaticIndexingOneToOneOwnedByContainedLazyOnContainedSideIT.IndexedEntity,
						AutomaticIndexingOneToOneOwnedByContainedLazyOnContainedSideIT.ContainingEntity,
						AutomaticIndexingOneToOneOwnedByContainedLazyOnContainedSideIT.ContainedEntity
				> {

	public AutomaticIndexingOneToOneOwnedByContainedLazyOnContainedSideIT() {
		super( new ModelPrimitivesImpl() );
	}

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		// Necessary for BytecodeEnhancerRunner, see BytecodeEnhancementIT.setup
		setupContext.withTcclLookupPrecedenceBefore();
	}

	@Override
	public void directAssociationUpdate_indexedEmbedded() {
		notTestedBecauseOfHSEARCH4305();
	}

	@Override
	public void directAssociationUpdate_indexedEmbeddedShallowReindexOnUpdate() {
		notTestedBecauseOfHSEARCH4305();
	}

	@Override
	public void indirectAssociationUpdate_indexedEmbedded() {
		notTestedBecauseOfHSEARCH4305();
	}

	@Override
	public void indirectAssociationUpdate_indexedEmbeddedShallowReindexOnUpdate() {
		notTestedBecauseOfHSEARCH4305();
	}

	@Override
	public void indirectAssociationUpdate_usedInCrossEntityDerivedProperty() {
		notTestedBecauseOfHSEARCH4305();
	}

	private void notTestedBecauseOfHSEARCH4305() {
		assumeTrue( "Association update tests fail because of https://hibernate.atlassian.net/browse/HSEARCH-4305",
				false );
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
			return true;
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
		public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbedded() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setContainingAsIndexedEmbedded,
					ContainedEntity::getContainingAsIndexedEmbedded );
		}

		@Override
		public PropertyAccessor<ContainingEntity, ContainedEntity> containedNonIndexedEmbedded() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setContainedNonIndexedEmbedded,
					ContainingEntity::getContainedNonIndexedEmbedded );
		}

		@Override
		public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsNonIndexedEmbedded() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setContainingAsNonIndexedEmbedded );
		}

		@Override
		public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedShallowReindexOnUpdate() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setContainedIndexedEmbeddedShallowReindexOnUpdate,
					ContainingEntity::getContainedIndexedEmbeddedShallowReindexOnUpdate );
		}

		@Override
		public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbeddedShallowReindexOnUpdate() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setContainingAsIndexedEmbeddedShallowReindexOnUpdate );
		}

		@Override
		public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedNoReindexOnUpdate() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setContainedIndexedEmbeddedNoReindexOnUpdate,
					ContainingEntity::getContainedIndexedEmbeddedNoReindexOnUpdate );
		}

		@Override
		public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbeddedNoReindexOnUpdate() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setContainingAsIndexedEmbeddedNoReindexOnUpdate );
		}

		@Override
		public PropertyAccessor<ContainingEntity, ContainedEntity> containedUsedInCrossEntityDerivedProperty() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setContainedUsedInCrossEntityDerivedProperty,
					ContainingEntity::getContainedUsedInCrossEntityDerivedProperty );
		}

		@Override
		public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsUsedInCrossEntityDerivedProperty() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setContainingAsUsedInCrossEntityDerivedProperty );
		}

		@Override
		public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedWithCast() {
			return new SingleValuedPropertyAccessor<>( ContainingEntity::setContainedIndexedEmbeddedWithCast );
		}

		@Override
		public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbeddedWithCast() {
			return new SingleValuedPropertyAccessor<>( ContainedEntity::setContainingAsIndexedEmbeddedWithCast );
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

		@OneToOne(fetch = FetchType.LAZY)
		private ContainingEntity parent;

		@OneToOne(mappedBy = "parent", fetch = FetchType.LAZY)
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

		@OneToOne(mappedBy = "containingAsIndexedEmbedded")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		private ContainedEntity containedIndexedEmbedded;

		@OneToOne(mappedBy = "containingAsNonIndexedEmbedded")
		private ContainedEntity containedNonIndexedEmbedded;

		@OneToOne(mappedBy = "containingAsIndexedEmbeddedShallowReindexOnUpdate")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
		private ContainedEntity containedIndexedEmbeddedShallowReindexOnUpdate;

		@OneToOne(mappedBy = "containingAsIndexedEmbeddedNoReindexOnUpdate")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		private ContainedEntity containedIndexedEmbeddedNoReindexOnUpdate;

		@OneToOne(mappedBy = "containingAsUsedInCrossEntityDerivedProperty")
		private ContainedEntity containedUsedInCrossEntityDerivedProperty;

		@OneToOne(mappedBy = "containingAsIndexedEmbeddedWithCast", targetEntity = ContainedEntity.class)
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

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "CIndexedEmbedded")
		private ContainingEntity containingAsIndexedEmbedded;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "CNonIndexedEmbedded")
		private ContainingEntity containingAsNonIndexedEmbedded;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "CIndexedEmbeddedSROU")
		private ContainingEntity containingAsIndexedEmbeddedShallowReindexOnUpdate;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "CIndexedEmbeddedNROU")
		private ContainingEntity containingAsIndexedEmbeddedNoReindexOnUpdate;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "CCrossEntityDerived")
		private ContainingEntity containingAsUsedInCrossEntityDerivedProperty;

		@OneToOne(targetEntity = ContainingEntity.class, fetch = FetchType.LAZY)
		@JoinColumn(name = "CIndexedEmbeddedCast")
		private Object containingAsIndexedEmbeddedWithCast;

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

		public ContainingEntity getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public void setContainingAsIndexedEmbedded(ContainingEntity containingAsIndexedEmbedded) {
			this.containingAsIndexedEmbedded = containingAsIndexedEmbedded;
		}

		public ContainingEntity getContainingAsNonIndexedEmbedded() {
			return containingAsNonIndexedEmbedded;
		}

		public void setContainingAsNonIndexedEmbedded(ContainingEntity containingAsNonIndexedEmbedded) {
			this.containingAsNonIndexedEmbedded = containingAsNonIndexedEmbedded;
		}

		public ContainingEntity getContainingAsIndexedEmbeddedShallowReindexOnUpdate() {
			return containingAsIndexedEmbeddedShallowReindexOnUpdate;
		}

		public void setContainingAsIndexedEmbeddedShallowReindexOnUpdate(
				ContainingEntity containingAsIndexedEmbeddedShallowReindexOnUpdate) {
			this.containingAsIndexedEmbeddedShallowReindexOnUpdate = containingAsIndexedEmbeddedShallowReindexOnUpdate;
		}

		public ContainingEntity getContainingAsIndexedEmbeddedNoReindexOnUpdate() {
			return containingAsIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainingAsIndexedEmbeddedNoReindexOnUpdate(
				ContainingEntity containingAsIndexedEmbeddedNoReindexOnUpdate) {
			this.containingAsIndexedEmbeddedNoReindexOnUpdate = containingAsIndexedEmbeddedNoReindexOnUpdate;
		}

		public ContainingEntity getContainingAsUsedInCrossEntityDerivedProperty() {
			return containingAsUsedInCrossEntityDerivedProperty;
		}

		public void setContainingAsUsedInCrossEntityDerivedProperty(
				ContainingEntity containingAsUsedInCrossEntityDerivedProperty) {
			this.containingAsUsedInCrossEntityDerivedProperty = containingAsUsedInCrossEntityDerivedProperty;
		}

		public Object getContainingAsIndexedEmbeddedWithCast() {
			return containingAsIndexedEmbeddedWithCast;
		}

		public void setContainingAsIndexedEmbeddedWithCast(Object containingAsIndexedEmbeddedWithCast) {
			this.containingAsIndexedEmbeddedWithCast = containingAsIndexedEmbeddedWithCast;
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
