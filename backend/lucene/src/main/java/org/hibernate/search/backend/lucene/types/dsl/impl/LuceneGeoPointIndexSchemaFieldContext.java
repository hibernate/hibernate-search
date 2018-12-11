/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneGeoPointFieldCodec;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneGeoPointFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneGeoPointFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneGeoPointFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * @author Guillaume Smet
 */
public class LuceneGeoPointIndexSchemaFieldContext
		extends AbstractLuceneStandardIndexSchemaFieldTypedContext<LuceneGeoPointIndexSchemaFieldContext, GeoPoint> {

	private Sortable sortable = Sortable.DEFAULT;

	public LuceneGeoPointIndexSchemaFieldContext(LuceneIndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName, GeoPoint.class );
	}

	@Override
	public LuceneGeoPointIndexSchemaFieldContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(IndexSchemaFieldDefinitionHelper<GeoPoint> helper, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		FromDocumentFieldValueConverter<? super GeoPoint, ?> indexToProjectionConverter =
				helper.createIndexToProjectionConverter();
		LuceneGeoPointFieldCodec codec = new LuceneGeoPointFieldCodec(
				parentNode.getAbsolutePath( getRelativeFieldName() ),
				resolvedProjectable, resolvedSortable
		);

		LuceneIndexSchemaFieldNode<GeoPoint> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getRelativeFieldName(),
				codec,
				LuceneGeoPointFieldPredicateBuilderFactory.INSTANCE,
				new LuceneGeoPointFieldSortBuilderFactory( resolvedSortable ),
				new LuceneGeoPointFieldProjectionBuilderFactory( resolvedProjectable, codec, indexToProjectionConverter )
		);

		helper.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}

	@Override
	protected LuceneGeoPointIndexSchemaFieldContext thisAsS() {
		return this;
	}
}
