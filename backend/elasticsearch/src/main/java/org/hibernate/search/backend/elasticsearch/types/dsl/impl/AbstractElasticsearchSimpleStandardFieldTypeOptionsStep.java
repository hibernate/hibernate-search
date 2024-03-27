/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;

abstract class AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<
		S extends AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<?, F>,
		F>
		extends AbstractElasticsearchStandardIndexFieldTypeOptionsStep<S, F> {

	private Sortable sortable = Sortable.DEFAULT;
	protected boolean resolvedSortable;

	private Projectable projectable = Projectable.DEFAULT;
	protected boolean resolvedProjectable;

	private Searchable searchable = Searchable.DEFAULT;
	protected boolean resolvedSearchable;

	private Aggregable aggregable = Aggregable.DEFAULT;
	protected boolean resolvedAggregable;

	private F indexNullAs;

	AbstractElasticsearchSimpleStandardFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType, String dataType) {
		super( buildContext, fieldType );
		builder.mapping().setType( dataType );
	}

	@Override
	public S projectable(Projectable projectable) {
		this.projectable = projectable;
		return thisAsS();
	}

	@Override
	public S sortable(Sortable sortable) {
		this.sortable = sortable;
		return thisAsS();
	}

	@Override
	public S indexNullAs(F indexNullAs) {
		this.indexNullAs = indexNullAs;
		return thisAsS();
	}

	@Override
	public S searchable(Searchable searchable) {
		this.searchable = searchable;
		return thisAsS();
	}

	@Override
	public S aggregable(Aggregable aggregable) {
		this.aggregable = aggregable;
		return thisAsS();
	}

	@Override
	public final IndexFieldType<F> toIndexFieldType() {
		PropertyMapping mapping = builder.mapping();

		resolvedSortable = resolveDefault( sortable );
		resolvedProjectable = resolveDefault( projectable );
		resolvedSearchable = resolveDefault( searchable );
		resolvedAggregable = resolveDefault( aggregable );

		mapping.setIndex( resolvedSearchable );
		mapping.setDocValues( resolvedSortable || resolvedAggregable );

		complete();

		if ( indexNullAs != null ) {
			builder.mapping().setNullValue( builder.codec().encode( indexNullAs ) );
		}

		return builder.build();
	}

	protected abstract void complete();

}
