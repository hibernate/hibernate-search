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

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.AbstractAutomaticIndexingSingleValuedAssociationBaseIT;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.ContainerPrimitives;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.MultiValuedPropertyAccessor;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.PropertyAccessor;
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
		extends AbstractAutomaticIndexingSingleValuedAssociationBaseIT<
						AutomaticIndexingOneToOneOwnedByContainedLazyOnContainedSideIT.IndexedEntity,
						AutomaticIndexingOneToOneOwnedByContainedLazyOnContainedSideIT.ContainingEntity,
						AutomaticIndexingOneToOneOwnedByContainedLazyOnContainedSideIT.ContainedEntity
				> {

	public AutomaticIndexingOneToOneOwnedByContainedLazyOnContainedSideIT() {
		super( IndexedEntity.PRIMITIVES, ContainingEntity.PRIMITIVES, ContainedEntity.PRIMITIVES );
	}

	@Override
	protected boolean isAssociationOwnedByContainedSide() {
		return true;
	}

	@Override
	protected boolean isAssociationLazyOnContainingSide() {
		return false;
	}

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		// Necessary for BytecodeEnhancerRunner, see BytecodeEnhancementIT.setup
		setupContext.withTcclLookupPrecedenceBefore();
	}

	@Override
	public void directAssociationUpdate_indexedEmbedded() {
		notTestedBecauseOfHSEARCH4305AndHSEARCH4708();
	}

	@Override
	public void directAssociationUpdate_indexedEmbeddedShallowReindexOnUpdate() {
		notTestedBecauseOfHSEARCH4305AndHSEARCH4708();
	}

	@Override
	public void indirectAssociationUpdate_indexedEmbedded() {
		notTestedBecauseOfHSEARCH4305AndHSEARCH4708();
	}

	@Override
	public void indirectAssociationUpdate_indexedEmbeddedShallowReindexOnUpdate() {
		notTestedBecauseOfHSEARCH4305AndHSEARCH4708();
	}

	@Override
	public void indirectAssociationUpdate_usedInCrossEntityDerivedProperty() {
		notTestedBecauseOfHSEARCH4305AndHSEARCH4708();
	}

	private void notTestedBecauseOfHSEARCH4305AndHSEARCH4708() {
		assumeTrue( "Association update tests fail because of https://hibernate.atlassian.net/browse/HSEARCH-4305 / https://hibernate.atlassian.net/browse/HSEARCH-4708",
				false );
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

		static final ContainingEntityPrimitives<ContainingEntity, ContainedEntity> PRIMITIVES = new ContainingEntityPrimitives<ContainingEntity, ContainedEntity>() {
			@Override
			public Class<ContainingEntity> entityClass() {
				return ContainingEntity.class;
			}

			@Override
			public ContainingEntity newInstance(int id) {
				ContainingEntity entity = new ContainingEntity();
				entity.setId( id );
				return entity;
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainingEntity> child() {
				return PropertyAccessor.create( ContainingEntity::setChild );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainingEntity> parent() {
				return PropertyAccessor.create( ContainingEntity::setParent );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbedded() {
				return PropertyAccessor.create( ContainingEntity::setContainedIndexedEmbedded,
						ContainingEntity::getContainedIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedNonIndexedEmbedded() {
				return PropertyAccessor.create( ContainingEntity::setContainedNonIndexedEmbedded,
						ContainingEntity::getContainedNonIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedShallowReindexOnUpdate() {
				return PropertyAccessor.create( ContainingEntity::setContainedIndexedEmbeddedShallowReindexOnUpdate,
						ContainingEntity::getContainedIndexedEmbeddedShallowReindexOnUpdate );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedNoReindexOnUpdate() {
				return PropertyAccessor.create( ContainingEntity::setContainedIndexedEmbeddedNoReindexOnUpdate,
						ContainingEntity::getContainedIndexedEmbeddedNoReindexOnUpdate );
			}


			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedUsedInCrossEntityDerivedProperty() {
				return PropertyAccessor.create( ContainingEntity::setContainedUsedInCrossEntityDerivedProperty,
						ContainingEntity::getContainedUsedInCrossEntityDerivedProperty );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedWithCast() {
				return PropertyAccessor.create( ContainingEntity::setContainedIndexedEmbeddedWithCast );
			}

			@Override
			public PropertyAccessor<ContainingEntity, String> nonIndexedField() {
				return PropertyAccessor.create( ContainingEntity::setNonIndexedField );
			}
		};
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity extends ContainingEntity {

		static final String INDEX = "IndexedEntity";

		static final IndexedEntityPrimitives<IndexedEntity> PRIMITIVES = new IndexedEntityPrimitives<IndexedEntity>() {
			@Override
			public Class<IndexedEntity> entityClass() {
				return IndexedEntity.class;
			}

			@Override
			public String indexName() {
				return IndexedEntity.INDEX;
			}

			@Override
			public IndexedEntity newInstance(int id) {
				IndexedEntity entity = new IndexedEntity();
				entity.setId( id );
				return entity;
			}
		};
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

		static ContainedEntityPrimitives<ContainedEntity, ContainingEntity> PRIMITIVES = new ContainedEntityPrimitives<ContainedEntity, ContainingEntity>() {
			@Override
			public Class<ContainedEntity> entityClass() {
				return ContainedEntity.class;
			}

			@Override
			public ContainedEntity newInstance(int id) {
				ContainedEntity entity = new ContainedEntity();
				entity.setId( id );
				return entity;
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbedded() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsIndexedEmbedded,
						ContainedEntity::getContainingAsIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsNonIndexedEmbedded() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsNonIndexedEmbedded,
						ContainedEntity::getContainingAsNonIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbeddedShallowReindexOnUpdate() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsIndexedEmbeddedShallowReindexOnUpdate,
						ContainedEntity::getContainingAsIndexedEmbeddedShallowReindexOnUpdate );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbeddedNoReindexOnUpdate() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsIndexedEmbeddedNoReindexOnUpdate,
						ContainedEntity::getContainingAsIndexedEmbeddedNoReindexOnUpdate );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsUsedInCrossEntityDerivedProperty() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsUsedInCrossEntityDerivedProperty,
						ContainedEntity::getContainingAsUsedInCrossEntityDerivedProperty );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbeddedWithCast() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsIndexedEmbeddedWithCast );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> indexedField() {
				return PropertyAccessor.create( ContainedEntity::setIndexedField );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> nonIndexedField() {
				return PropertyAccessor.create( ContainedEntity::setNonIndexedField );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, String, List<String>> indexedElementCollectionField() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getIndexedElementCollectionField,
						ContainedEntity::setIndexedElementCollectionField );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, String, List<String>> nonIndexedElementCollectionField() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getNonIndexedElementCollectionField,
						ContainedEntity::setNonIndexedElementCollectionField );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> fieldUsedInContainedDerivedField1() {
				return PropertyAccessor.create( ContainedEntity::setFieldUsedInContainedDerivedField1 );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> fieldUsedInContainedDerivedField2() {
				return PropertyAccessor.create( ContainedEntity::setFieldUsedInContainedDerivedField2 );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> fieldUsedInCrossEntityDerivedField1() {
				return PropertyAccessor.create( ContainedEntity::setFieldUsedInCrossEntityDerivedField1 );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> fieldUsedInCrossEntityDerivedField2() {
				return PropertyAccessor.create( ContainedEntity::setFieldUsedInCrossEntityDerivedField2 );
			}
		};
	}

}
