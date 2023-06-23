/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Test;

public abstract class AbstractPredicateScaleCheckingIT {

	private final SimpleMappedIndex<IndexBinding> index;
	private final SimpleMappedIndex<IndexBinding> compatibleIndex;
	private final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex;
	private final DataSet dataSet;

	protected AbstractPredicateScaleCheckingIT(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<IndexBinding> compatibleIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet dataSet) {
		this.index = index;
		this.compatibleIndex = compatibleIndex;
		this.incompatibleIndex = incompatibleIndex;
		this.dataSet = dataSet;
	}

	@Test
	public void multiIndex_withCompatibleIndex() {
		StubMappingScope scope = index.createScope( compatibleIndex );

		assertThatQuery( scope.query()
				.where( f -> predicate( f, bigDecimalFieldPath(), dataSet.bigDecimal0 ) ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
					b.doc( compatibleIndex.typeName(), dataSet.docId( 0 ) );
				} );

		assertThatQuery( scope.query()
				.where( f -> predicate( f, bigIntegerFieldPath(), dataSet.bigInteger0 ) ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
					b.doc( compatibleIndex.typeName(), dataSet.docId( 0 ) );
				} );
	}

	@Test
	public void multiIndex_withIncompatibleIndex() {
		StubMappingScope scope = index.createScope( incompatibleIndex );

		assertThatThrownBy( () -> predicate( scope.predicate(), bigDecimalFieldPath(), dataSet.bigDecimal0 ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + bigDecimalFieldPath()
								+ "' in a search query across multiple indexes",
						"Inconsistent support for '" + predicateNameInErrorMessage() + "'",
						"Field codec differs:", "decimalScale=2", " vs. ", "decimalScale=7"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), incompatibleIndex.name() )
				) );

		assertThatThrownBy( () -> predicate( scope.predicate(), bigIntegerFieldPath(), dataSet.bigInteger0 ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + bigIntegerFieldPath()
								+ "' in a search query across multiple indexes",
						"Inconsistent support for '" + predicateNameInErrorMessage() + "'",
						"Field codec differs:", "decimalScale=-2", " vs. ", "decimalScale=-7"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), incompatibleIndex.name() )
				) );
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, Object matchingParam);

	protected abstract String predicateNameInErrorMessage();

	private String bigDecimalFieldPath() {
		return "bigDecimal";
	}

	private String bigIntegerFieldPath() {
		return "bigInteger";
	}

	public static final class IndexBinding {
		private final IndexFieldReference<BigDecimal> bigDecimalReference;
		private final IndexFieldReference<BigInteger> bigIntegerReference;

		public IndexBinding(IndexSchemaElement root) {
			bigDecimalReference = root.field( "bigDecimal", c -> c.asBigDecimal().decimalScale( 2 ) )
					.toReference();
			bigIntegerReference = root.field( "bigInteger", c -> c.asBigInteger().decimalScale( -2 ) )
					.toReference();
		}
	}

	public static final class IncompatibleIndexBinding {
		public IncompatibleIndexBinding(IndexSchemaElement root) {
			// Use different scales than in IndexBinding
			root.field( "bigDecimal", c -> c.asBigDecimal().decimalScale( 7 ) ).toReference();
			root.field( "bigInteger", c -> c.asBigInteger().decimalScale( -7 ) ).toReference();
		}
	}

	public static final class DataSet extends AbstractPredicateDataSet {
		private final BigDecimal bigDecimal0 = new BigDecimal( "10.50" );
		private final BigInteger bigInteger0 = BigInteger.valueOf( 250_000L );

		public DataSet() {
			super( null );
		}

		public void contribute(SimpleMappedIndex<IndexBinding> mainIndex, BulkIndexer mainIndexer,
				SimpleMappedIndex<IndexBinding> compatibleIndex, BulkIndexer compatibleIndexer) {
			mainIndexer.add( docId( 0 ), routingKey,
					document -> initDocument( mainIndex, document, bigDecimal0, bigInteger0 ) );
			mainIndexer.add( docId( 1 ), routingKey,
					document -> initDocument( mainIndex, document, null, null ) );
			compatibleIndexer.add( docId( 0 ), routingKey,
					document -> initDocument( compatibleIndex, document, bigDecimal0, bigInteger0 ) );
			compatibleIndexer.add( docId( 1 ), routingKey,
					document -> initDocument( compatibleIndex, document, null, null ) );
		}

		private void initDocument(SimpleMappedIndex<IndexBinding> index, DocumentElement document,
				BigDecimal bigDecimalValue, BigInteger bigIntegerValue) {
			IndexBinding binding = index.binding();
			document.addValue( binding.bigDecimalReference, bigDecimalValue );
			document.addValue( binding.bigIntegerReference, bigIntegerValue );
		}
	}
}
