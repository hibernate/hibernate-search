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
import org.hibernate.search.backend.lucene.types.codec.impl.GeoPointFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.impl.StandardFieldConverter;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneGeoPointFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.GeoPointFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneGeoPointFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * @author Guillaume Smet
 */
public class LuceneGeoPointIndexSchemaFieldContextImpl
		extends AbstractLuceneStandardIndexSchemaFieldTypedContext<LuceneGeoPointIndexSchemaFieldContextImpl, GeoPoint> {

	private Sortable sortable = Sortable.DEFAULT;

	public LuceneGeoPointIndexSchemaFieldContextImpl(LuceneIndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName, GeoPoint.class );
	}

	@Override
	public LuceneGeoPointIndexSchemaFieldContextImpl sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(IndexSchemaFieldDefinitionHelper<GeoPoint> helper, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		StandardFieldConverter<GeoPoint> converter = new StandardFieldConverter<>(
				helper.createUserIndexFieldConverter() );
		GeoPointFieldCodec codec = new GeoPointFieldCodec( parentNode.getAbsolutePath( getRelativeFieldName() ),
				resolvedProjectable, resolvedSortable );

		LuceneIndexSchemaFieldNode<GeoPoint> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getRelativeFieldName(),
				converter,
				codec,
				LuceneGeoPointFieldPredicateBuilderFactory.INSTANCE,
				new LuceneGeoPointFieldSortBuilderFactory( resolvedSortable ),
				new GeoPointFieldProjectionBuilderFactory<>( resolvedProjectable, codec, converter )
		);

		helper.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}

	@Override
	protected LuceneGeoPointIndexSchemaFieldContextImpl thisAsS() {
		return this;
	}
}
