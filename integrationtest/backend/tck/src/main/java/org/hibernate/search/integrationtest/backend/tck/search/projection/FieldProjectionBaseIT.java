/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class FieldProjectionBaseIT {
	//CHECKSTYLE:ON

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static <F> FieldProjectionTestValues<F> testValues(FieldTypeDescriptor<F, ?> fieldType) {
		return new FieldProjectionTestValues<>( fieldType );
	}

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes( InObjectProjectionConfigured.mainIndex, InObjectProjectionConfigured.missingLevel1Index,
						InObjectProjectionConfigured.missingLevel1SingleValuedFieldIndex,
						InObjectProjectionConfigured.missingLevel2Index,
						InObjectProjectionConfigured.missingLevel2SingleValuedFieldIndex )
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
	class InObjectProjectionIT<F> extends InObjectProjectionConfigured<F> {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InObjectProjectionConfigured<F>
			extends AbstractProjectionInObjectProjectionIT<F, F, FieldProjectionTestValues<F>> {
		private static final List<StandardFieldTypeDescriptor<?>> supportedFieldTypes = FieldTypeDescriptor.getAllStandard();
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

		private static final List<DataSet<?, ?, ?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
				for ( ObjectStructure singleValuedObjectStructure : new ObjectStructure[] {
						ObjectStructure.FLATTENED,
						ObjectStructure.NESTED } ) {
					ObjectStructure multiValuedObjectStructure =
							ObjectStructure.NESTED.equals( singleValuedObjectStructure )
									|| TckConfiguration.get().getBackendFeatures()
											.reliesOnNestedDocumentsForMultiValuedObjectProjection()
													? ObjectStructure.NESTED
													: ObjectStructure.FLATTENED;
					DataSet<?, ?, ?> dataSet =
							new DataSet<>( testValues( fieldType ), singleValuedObjectStructure, multiValuedObjectStructure );
					dataSets.add( dataSet );
					parameters.add( Arguments.of( mainIndex, missingLevel1Index, missingLevel1SingleValuedFieldIndex,
							missingLevel2Index,
							missingLevel2SingleValuedFieldIndex,
							dataSet ) );
				}
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected RecursiveComparisonConfiguration.Builder configureRecursiveComparison(
				RecursiveComparisonConfiguration.Builder builder) {
			return builder.withComparatorForType( Comparator.nullsFirst( Comparator.naturalOrder() ), BigDecimal.class );
		}

		@Override
		protected ProjectionFinalStep<F> singleValuedProjection(SearchProjectionFactory<?, ?> f,
				String absoluteFieldPath, DataSet<F, F, FieldProjectionTestValues<F>> dataSet) {
			return f.field( absoluteFieldPath, dataSet.fieldType.getJavaType() );
		}

		@Override
		protected ProjectionFinalStep<List<F>> multiValuedProjection(SearchProjectionFactory<?, ?> f,
				String absoluteFieldPath, DataSet<F, F, FieldProjectionTestValues<F>> dataSet) {
			return f.field( absoluteFieldPath, dataSet.fieldType.getJavaType() ).multi();
		}

	}
}
