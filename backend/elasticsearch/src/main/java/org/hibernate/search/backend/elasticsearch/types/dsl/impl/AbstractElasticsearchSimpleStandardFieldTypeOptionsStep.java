/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;


abstract class AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<S extends AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<? extends S, F>, F>
		extends AbstractElasticsearchStandardIndexFieldTypeOptionsStep<S, F> {

	private final String dataType;

	private Sortable sortable = Sortable.DEFAULT;
	protected boolean resolvedSortable;

	private Projectable projectable = Projectable.DEFAULT;
	protected boolean resolvedProjectable;

	private Searchable searchable = Searchable.DEFAULT;
	protected boolean resolvedSearchable;

	private F indexNullAs;

	AbstractElasticsearchSimpleStandardFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType, String dataType) {
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
		this.searchable = searchable;
		return thisAsS();
	}

	@Override
	public S aggregable(Aggregable aggregable) {
		throw new UnsupportedOperationException( "Not supported yet" );
	}

	@Override
	public final IndexFieldType<F> toIndexFieldType() {
		PropertyMapping mapping = new PropertyMapping();

		mapping.setType( dataType );

		resolvedSortable = resolveDefault( sortable );
		resolvedProjectable = resolveDefault( projectable );
		resolvedSearchable = resolveDefault( searchable );

		mapping.setIndex( resolvedSearchable );
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
