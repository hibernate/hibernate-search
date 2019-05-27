/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;

/**
 * @author Yoann Rodiere
 */
abstract class AbstractElasticsearchScalarFieldTypeContext<S extends AbstractElasticsearchScalarFieldTypeContext<? extends S, F>, F>
		extends AbstractElasticsearchStandardIndexFieldTypeContext<S, F> {

	private final DataType dataType;

	private Sortable sortable = Sortable.DEFAULT;
	protected boolean resolvedSortable;

	private Projectable projectable = Projectable.DEFAULT;
	protected boolean resolvedProjectable;

	protected F indexNullAs;

	AbstractElasticsearchScalarFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType, DataType dataType) {
		super( buildContext, fieldType );
		this.dataType = dataType;
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
		// TODO HSEARCH-3048 (current) contribute searchable
		return thisAsS();
	}

	@Override
	public final IndexFieldType<F> toIndexFieldType() {
		PropertyMapping mapping = new PropertyMapping();

		mapping.setType( dataType );

		resolvedSortable = resolveDefault( sortable );
		resolvedProjectable = resolveDefault( projectable );

		// TODO HSEARCH-3048 allow to configure indexed/not indexed
		mapping.setIndex( true );
		mapping.setStore( resolvedProjectable );
		mapping.setDocValues( resolvedSortable );

		ElasticsearchIndexFieldType<F> indexFieldType = toIndexFieldType( mapping );
		if ( indexNullAs != null ) {
			indexFieldType.indexNullAs( indexNullAs );
		}
		return indexFieldType;
	}

	protected abstract ElasticsearchIndexFieldType<F> toIndexFieldType(PropertyMapping mapping);

}
