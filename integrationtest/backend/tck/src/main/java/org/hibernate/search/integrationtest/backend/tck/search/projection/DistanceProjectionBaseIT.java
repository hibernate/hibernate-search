/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class DistanceProjectionBaseIT {
	//CHECKSTYLE:ON

	private static final GeoPointFieldTypeDescriptor supportedFieldType;
	private static final List<
			FieldTypeDescriptor<GeoPoint, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> supportedFieldTypes =
					new ArrayList<>();
	private static final List<
			FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> unsupportedFieldTypes =
					new ArrayList<>();
	static {
		supportedFieldType = GeoPointFieldTypeDescriptor.INSTANCE;
		supportedFieldTypes.add( supportedFieldType );
		for ( FieldTypeDescriptor<?, ?> fieldType : FieldTypeDescriptor.getAll() ) {
			if ( !supportedFieldType.equals( fieldType ) ) {
				unsupportedFieldTypes.add( fieldType );
			}
		}
	}

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static DistanceProjectionTestValues testValues() {
		return new DistanceProjectionTestValues();
	}

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes( InObjectProjectionConfigured.mainIndex, InObjectProjectionConfigured.missingLevel1Index,
						InObjectProjectionConfigured.missingLevel1SingleValuedFieldIndex,
						InObjectProjectionConfigured.missingLevel2Index,
						InObjectProjectionConfigured.missingLevel2SingleValuedFieldIndex,
						InvalidFieldConfigured.index, UnsupportedTypeConfigured.index,
						ProjectableConfigured.projectableDefaultIndex, ProjectableConfigured.projectableYesIndex,
						ProjectableConfigured.projectableNoIndex )
				.setup();

		BulkIndexer compositeForEachMainIndexer = InObjectProjectionConfigured.mainIndex.bulkIndexer();
		BulkIndexer compositeForEachMissingLevel1Indexer = InObjectProjectionConfigured.missingLevel1Index.bulkIndexer();
		BulkIndexer compositeForEachMissingLevel1SingleValuedFieldIndexer =
				InObjectProjectionConfigured.missingLevel1SingleValuedFieldIndex.bulkIndexer();
		BulkIndexer compositeForEachMissingLevel2Indexer = InObjectProjectionConfigured.missingLevel2Index.bulkIndexer();
		BulkIndexer compositeForEachMissingLevel2SingleValuedFieldIndexer =
				InObjectProjectionConfigured.missingLevel2SingleValuedFieldIndex.bulkIndexer();
		InObjectProjectionConfigured.dataSets
				.forEach( d -> d.contribute( InObjectProjectionConfigured.mainIndex, compositeForEachMainIndexer,
						InObjectProjectionConfigured.missingLevel1Index, compositeForEachMissingLevel1Indexer,
						InObjectProjectionConfigured.missingLevel1SingleValuedFieldIndex,
						compositeForEachMissingLevel1SingleValuedFieldIndexer,
						InObjectProjectionConfigured.missingLevel2Index, compositeForEachMissingLevel2Indexer,
						InObjectProjectionConfigured.missingLevel2SingleValuedFieldIndex,
						compositeForEachMissingLevel2SingleValuedFieldIndexer ) );

		compositeForEachMainIndexer.join( compositeForEachMissingLevel1Indexer,
				compositeForEachMissingLevel1SingleValuedFieldIndexer, compositeForEachMissingLevel2Indexer,
				compositeForEachMissingLevel2SingleValuedFieldIndexer );
	}

	@Nested
	class InObjectProjectionIT extends InObjectProjectionConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InObjectProjectionConfigured
			extends AbstractProjectionInObjectProjectionIT<GeoPoint, Double, DistanceProjectionTestValues> {
		private static final SimpleMappedIndex<IndexBinding> mainIndex =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "main" );
		private static final SimpleMappedIndex<MissingLevel1IndexBinding> missingLevel1Index =
				SimpleMappedIndex.of( MissingLevel1IndexBinding::new )
						.name( "missingLevel1" );
		private static final SimpleMappedIndex<MissingLevel1SingleValuedFieldIndexBinding> missingLevel1SingleValuedFieldIndex =
				SimpleMappedIndex.of( root -> new MissingLevel1SingleValuedFieldIndexBinding( root, supportedFieldTypes ) )
						.name( "missingLevel1Field1" );
		private static final SimpleMappedIndex<MissingLevel2IndexBinding> missingLevel2Index =
				SimpleMappedIndex.of( root -> new MissingLevel2IndexBinding( root, supportedFieldTypes ) )
						.name( "missingLevel2" );
		private static final SimpleMappedIndex<MissingLevel2SingleValuedFieldIndexBinding> missingLevel2SingleValuedFieldIndex =
				SimpleMappedIndex.of( root -> new MissingLevel2SingleValuedFieldIndexBinding( root, supportedFieldTypes ) )
						.name( "missingLevel2Field1" );

		private static final List<DataSet<GeoPoint, Double, DistanceProjectionTestValues>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( ObjectStructure singleValuedObjectStructure : new ObjectStructure[] {
					ObjectStructure.FLATTENED,
					ObjectStructure.NESTED } ) {
				ObjectStructure multiValuedObjectStructure =
						ObjectStructure.NESTED.equals( singleValuedObjectStructure )
								|| TckConfiguration.get().getBackendFeatures()
										.reliesOnNestedDocumentsForMultiValuedObjectProjection()
												? ObjectStructure.NESTED
												: ObjectStructure.FLATTENED;
				DataSet<GeoPoint, Double, DistanceProjectionTestValues> dataSet = new DataSet<>( testValues(),
						singleValuedObjectStructure, multiValuedObjectStructure );
				dataSets.add( dataSet );
				parameters.add(
						Arguments.of( mainIndex, missingLevel1Index, missingLevel1SingleValuedFieldIndex, missingLevel2Index,
								missingLevel2SingleValuedFieldIndex,
								dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected RecursiveComparisonConfiguration.Builder configureRecursiveComparison(
				RecursiveComparisonConfiguration.Builder builder) {
			return builder.withComparatorForType( TestComparators.APPROX_M_COMPARATOR, Double.class );
		}

		@Override
		protected ProjectionFinalStep<Double> singleValuedProjection(SearchProjectionFactory<?, ?> f,
				String absoluteFieldPath, DataSet<GeoPoint, Double, DistanceProjectionTestValues> dataSet) {
			return f.distance( absoluteFieldPath, dataSet.values.projectionCenterPoint() );
		}

		@Override
		protected ProjectionFinalStep<List<Double>> multiValuedProjection(SearchProjectionFactory<?, ?> f,
				String absoluteFieldPath, DataSet<GeoPoint, Double, DistanceProjectionTestValues> dataSet) {
			return f.distance( absoluteFieldPath, dataSet.values.projectionCenterPoint() ).multi();
		}

	}

	@Nested
	class InvalidFieldIT extends InvalidFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InvalidFieldConfigured extends AbstractProjectionInvalidFieldIT {
		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "invalidField" );

		public InvalidFieldConfigured() {
			super( index );
		}

		@Override
		protected void tryProjection(SearchProjectionFactory<?, ?> f, String fieldPath) {
			f.distance( fieldPath, GeoPoint.of( 0.0, 0.0 ) );
		}

		@Override
		protected String projectionTrait() {
			return "projection:distance";
		}
	}

	@Nested
	class UnsupportedTypeIT extends UnsupportedTypeConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class UnsupportedTypeConfigured extends AbstractProjectionUnsupportedTypesIT {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, unsupportedFieldTypes ) )
						.name( "unsupportedType" );

		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : unsupportedFieldTypes ) {
				parameters.add( Arguments.of( index, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected void tryProjection(SearchProjectionFactory<?, ?> f, String fieldPath) {
			f.distance( fieldPath, GeoPoint.of( 0.0, 0.0 ) );
		}

		@Override
		protected String projectionTrait() {
			return "projection:distance";
		}
	}

	@Nested
	class ProjectableIT extends ProjectableConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class ProjectableConfigured extends AbstractProjectionProjectableIT {
		private static final SimpleMappedIndex<ProjectableDefaultIndexBinding> projectableDefaultIndex =
				SimpleMappedIndex.of( root -> new ProjectableDefaultIndexBinding( root, supportedFieldTypes ) )
						.name( "projectableDefault" );
		private static final SimpleMappedIndex<ProjectableYesIndexBinding> projectableYesIndex =
				SimpleMappedIndex.of( root -> new ProjectableYesIndexBinding( root, supportedFieldTypes ) )
						.name( "projectableYes" );

		private static final SimpleMappedIndex<ProjectableNoIndexBinding> projectableNoIndex =
				SimpleMappedIndex.of( root -> new ProjectableNoIndexBinding( root, supportedFieldTypes ) )
						.name( "projectableNo" );

		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
				parameters.add( Arguments.of( projectableDefaultIndex, projectableYesIndex, projectableNoIndex, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected void tryProjection(SearchProjectionFactory<?, ?> f, String fieldPath, FieldTypeDescriptor<?, ?> fieldType) {
			f.distance( fieldPath, GeoPoint.of( 0.0, 0.0 ) );
		}

		@Override
		protected String projectionTrait() {
			return "projection:distance";
		}
	}
}
