/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldProjection;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneNumericTermsAggregation;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBooleanFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneExistsPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericMatchPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericRangePredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericTermsPredicate;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneStandardFieldSort;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;

class LuceneBooleanIndexFieldTypeOptionsStep
		extends AbstractLuceneStandardIndexFieldTypeOptionsStep<LuceneBooleanIndexFieldTypeOptionsStep, Boolean> {

	private Sortable sortable = Sortable.DEFAULT;

	LuceneBooleanIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Boolean.class );
	}

	@Override
	public LuceneBooleanIndexFieldTypeOptionsStep sortable(Sortable sortable) {
		this.sortable = sortable;
		return thisAsS();
	}

	@Override
	public LuceneIndexValueFieldType<Boolean> toIndexFieldType() {
		boolean resolvedSearchable = resolveDefault( searchable );
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedAggregable = resolveDefault( aggregable );

		Indexing indexing = resolvedSearchable ? Indexing.ENABLED : Indexing.DISABLED;
		DocValues docValues = resolvedSortable || resolvedAggregable ? DocValues.ENABLED : DocValues.DISABLED;
		Storage storage = resolvedProjectable ? Storage.ENABLED : Storage.DISABLED;

		LuceneBooleanFieldCodec codec = new LuceneBooleanFieldCodec( indexing, docValues, storage, indexNullAsValue );
		builder.codec( codec );

		if ( resolvedSearchable ) {
			builder.searchable( true );
			builder.queryElementFactory( PredicateTypeKeys.MATCH, new LuceneNumericMatchPredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.RANGE, new LuceneNumericRangePredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.TERMS, new LuceneNumericTermsPredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.EXISTS,
					DocValues.ENABLED.equals( docValues )
							? new LuceneExistsPredicate.DocValuesOrNormsBasedFactory<>()
							: new LuceneExistsPredicate.DefaultFactory<>() );
		}

		if ( resolvedSortable ) {
			builder.sortable( true );
			builder.queryElementFactory( SortTypeKeys.FIELD, new LuceneStandardFieldSort.NumericFieldFactory<>( codec ) );
		}

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new LuceneFieldProjection.Factory<>( codec ) );
		}

		if ( resolvedAggregable ) {
			builder.aggregable( true );
			builder.queryElementFactory( AggregationTypeKeys.TERMS, new LuceneNumericTermsAggregation.Factory<>( codec ) );
		}

		return builder.build();
	}

	@Override
	protected LuceneBooleanIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

}
