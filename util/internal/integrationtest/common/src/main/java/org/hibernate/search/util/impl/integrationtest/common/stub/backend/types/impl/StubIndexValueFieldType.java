/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexValueFieldType;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl.StubSearchAggregation;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexValueFieldContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexValueFieldTypeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl.StubSearchPredicate;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubDistanceToFieldProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubFieldHighlightProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubFieldProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.impl.StubSearchSort;

public final class StubIndexValueFieldType<F>
		extends AbstractIndexValueFieldType<
				StubSearchIndexScope,
				StubSearchIndexValueFieldContext<F>,
				F>
		implements IndexFieldType<F>, StubSearchIndexValueFieldTypeContext<F> {

	private final List<Consumer<StubIndexSchemaDataNode.Builder>> modifiers;

	public StubIndexValueFieldType(Builder<F> builder) {
		super( builder );
		this.modifiers = new ArrayList<>( builder.modifiers );
	}

	public void apply(StubIndexSchemaDataNode.Builder builder) {
		builder.valueClass( valueClass() );
		for ( Consumer<StubIndexSchemaDataNode.Builder> modifier : modifiers ) {
			modifier.accept( builder );
		}
	}

	public static class Builder<F>
			extends AbstractIndexValueFieldType.Builder<
					StubSearchIndexScope,
					StubSearchIndexValueFieldContext<F>,
					F> {
		private final List<Consumer<StubIndexSchemaDataNode.Builder>> modifiers = new ArrayList<>();

		public Builder(Class<F> valueClass) {
			super( valueClass );
			stubFactories(
					new StubSearchPredicate.Factory(),
					PredicateTypeKeys.NESTED,
					PredicateTypeKeys.MATCH,
					PredicateTypeKeys.RANGE,
					PredicateTypeKeys.EXISTS,
					PredicateTypeKeys.PHRASE,
					PredicateTypeKeys.WILDCARD,
					PredicateTypeKeys.TERMS,
					PredicateTypeKeys.SPATIAL_WITHIN_CIRCLE,
					PredicateTypeKeys.SPATIAL_WITHIN_POLYGON,
					PredicateTypeKeys.SPATIAL_WITHIN_BOUNDING_BOX
			);
			stubFactories(
					new StubSearchPredicate.RegexpFactory(),
					PredicateTypeKeys.REGEXP
			);
			stubFactories(
					new StubSearchSort.Factory(),
					SortTypeKeys.FIELD,
					SortTypeKeys.DISTANCE
			);
			queryElementFactory( ProjectionTypeKeys.FIELD, new StubFieldProjection.Factory() );
			queryElementFactory( ProjectionTypeKeys.DISTANCE, new StubDistanceToFieldProjection.Factory() );
			queryElementFactory( ProjectionTypeKeys.HIGHLIGHT, new StubFieldHighlightProjection.Factory() );
			queryElementFactory( AggregationTypeKeys.TERMS, new StubSearchAggregation.TermsFactory() );
			queryElementFactory( AggregationTypeKeys.RANGE, new StubSearchAggregation.RangeFactory() );
		}

		// Needs to be final even if private, to avoid errors with javac.
		@SafeVarargs
		private final <T> void stubFactories(AbstractStubSearchQueryElementFactory<T> factory,
				SearchQueryElementTypeKey<? super T>... keys) {
			for ( SearchQueryElementTypeKey<? super T> key : keys ) {
				queryElementFactory( key, factory );
			}
		}

		public void modifier(Consumer<StubIndexSchemaDataNode.Builder> modifier) {
			modifiers.add( modifier );
		}

		@Override
		public StubIndexValueFieldType<F> build() {
			return new StubIndexValueFieldType<>( this );
		}
	}

}
