/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.backend.elasticsearch.util.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class ElasticsearchSearchTargetModel {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<ElasticsearchIndexModel> indexModels;
	private final Set<URLEncodedString> indexNames;

	public ElasticsearchSearchTargetModel(Set<ElasticsearchIndexModel> indexModels) {
		this.indexModels = indexModels;
		this.indexNames = indexModels.stream()
				.map( ElasticsearchIndexModel::getIndexName )
				.collect( Collectors.toSet() );
	}

	public Set<URLEncodedString> getIndexNames() {
		return indexNames;
	}

	public Set<ElasticsearchIndexModel> getIndexModels() {
		return indexModels;
	}

	public ElasticsearchFieldCodec getFieldCodec(String absoluteFieldPath) {
		ElasticsearchIndexModel indexModelForSelectedCodec = null;
		ElasticsearchFieldCodec selectedCodec = null;
		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			ElasticsearchIndexSchemaFieldNode schemaNode = indexModel.getFieldNode( absoluteFieldPath );
			if ( schemaNode != null ) {
				ElasticsearchFieldCodec codec = schemaNode.getCodec();
				if ( selectedCodec == null ) {
					selectedCodec = codec;
					indexModelForSelectedCodec = indexModel;
				}
				else if ( !selectedCodec.equals( codec ) ) {
					throw log.conflictingFieldCodecsForSearch(
							absoluteFieldPath,
							selectedCodec, indexModelForSelectedCodec.getIndexName(),
							codec, indexModel.getIndexName()
					);
				}
			}
		}
		if ( selectedCodec == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexNames() );
		}
		return selectedCodec;
	}

	public void checkNestedField(String absoluteFieldPath) {
		boolean found = false;

		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			ElasticsearchIndexSchemaObjectNode schemaNode = indexModel.getObjectNode( absoluteFieldPath );
			if ( schemaNode != null ) {
				found = true;
				if ( !ObjectFieldStorage.NESTED.equals( schemaNode.getStorage() ) ) {
					throw log.nonNestedFieldForNestedQuery( indexModel.getIndexName(), absoluteFieldPath );
				}
			}
		}
		if ( !found ) {
			for ( ElasticsearchIndexModel indexModel : indexModels ) {
				ElasticsearchIndexSchemaFieldNode schemaNode = indexModel.getFieldNode( absoluteFieldPath );
				if ( schemaNode != null ) {
					throw log.nonObjectFieldForNestedQuery( indexModel.getIndexName(), absoluteFieldPath );
				}
			}
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexNames() );
		}
	}
}
