/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.runner.nested.Nested;
import org.hibernate.search.util.impl.test.runner.nested.NestedRunner;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;

@RunWith(NestedRunner.class)
public class DistanceProjectionBaseIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static DistanceProjectionTestValues testValues() {
		return new DistanceProjectionTestValues();
	}

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndexes( InObjectProjectionIT.mainIndex, InObjectProjectionIT.missingLevel1Index,
						InObjectProjectionIT.missingLevel1SingleValuedFieldIndex,
						InObjectProjectionIT.missingLevel2Index, InObjectProjectionIT.missingLevel2SingleValuedFieldIndex )
				.setup();

		BulkIndexer compositeForEachMainIndexer = InObjectProjectionIT.mainIndex.bulkIndexer();
		BulkIndexer compositeForEachMissingLevel1Indexer = InObjectProjectionIT.missingLevel1Index.bulkIndexer();
		BulkIndexer compositeForEachMissingLevel1SingleValuedFieldIndexer = InObjectProjectionIT.missingLevel1SingleValuedFieldIndex.bulkIndexer();
		BulkIndexer compositeForEachMissingLevel2Indexer = InObjectProjectionIT.missingLevel2Index.bulkIndexer();
		BulkIndexer compositeForEachMissingLevel2SingleValuedFieldIndexer = InObjectProjectionIT.missingLevel2SingleValuedFieldIndex.bulkIndexer();
		InObjectProjectionIT.dataSets.forEach( d -> d.contribute( InObjectProjectionIT.mainIndex, compositeForEachMainIndexer,
				InObjectProjectionIT.missingLevel1Index, compositeForEachMissingLevel1Indexer,
				InObjectProjectionIT.missingLevel1SingleValuedFieldIndex, compositeForEachMissingLevel1SingleValuedFieldIndexer,
				InObjectProjectionIT.missingLevel2Index, compositeForEachMissingLevel2Indexer,
				InObjectProjectionIT.missingLevel2SingleValuedFieldIndex, compositeForEachMissingLevel2SingleValuedFieldIndexer ) );

		compositeForEachMainIndexer.join( compositeForEachMissingLevel1Indexer,
				compositeForEachMissingLevel1SingleValuedFieldIndexer, compositeForEachMissingLevel2Indexer,
				compositeForEachMissingLevel2SingleValuedFieldIndexer );
	}

	@Test
	public void takariCpSuiteWorkaround() {
		// Workaround to get Takari-CPSuite to run this test.
	}

	@Nested
	@RunWith(Parameterized.class)
	public static class InObjectProjectionIT
			extends AbstractProjectionInObjectProjectionIT<GeoPoint, Double, DistanceProjectionTestValues> {
		private static final List<FieldTypeDescriptor<?>> supportedFieldTypes =
				Arrays.asList( GeoPointFieldTypeDescriptor.INSTANCE );
		private static final List<DataSet<GeoPoint, Double, DistanceProjectionTestValues>> dataSets = new ArrayList<>();
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( ObjectStructure structure :
					TckConfiguration.get().getBackendFeatures().reliesOnNestedDocumentsForObjectProjection()
							? new ObjectStructure[] { ObjectStructure.NESTED }
							: new ObjectStructure[] { ObjectStructure.FLATTENED, ObjectStructure.NESTED } ) {
				DataSet<GeoPoint, Double, DistanceProjectionTestValues> dataSet = new DataSet<>( testValues(), structure );
				dataSets.add( dataSet );
				parameters.add( new Object[] { dataSet } );
			}
		}

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

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		public InObjectProjectionIT(DataSet<GeoPoint, Double, DistanceProjectionTestValues> dataSet) {
			super( mainIndex, missingLevel1Index, missingLevel1SingleValuedFieldIndex, missingLevel2Index,
					missingLevel2SingleValuedFieldIndex,
					dataSet );
		}

		@Override
		protected RecursiveComparisonConfiguration.Builder configureRecursiveComparison(
				RecursiveComparisonConfiguration.Builder builder) {
			return builder.withComparatorForType( TestComparators.APPROX_M_COMPARATOR, Double.class );
		}

		@Override
		protected ProjectionFinalStep<Double> singleValuedProjection(SearchProjectionFactory<?, ?> f,
				String absoluteFieldPath) {
			return f.distance( absoluteFieldPath, dataSet.values.projectionCenterPoint() );
		}

		@Override
		protected ProjectionFinalStep<List<Double>> multiValuedProjection(SearchProjectionFactory<?, ?> f,
				String absoluteFieldPath) {
			return f.distance( absoluteFieldPath, dataSet.values.projectionCenterPoint() ).multi();
		}

	}
}
