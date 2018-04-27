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

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.impl.URLEncodedString;
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

	public ElasticsearchIndexSchemaFieldNode getSchemaNode(String absoluteFieldPath) {
		ElasticsearchIndexModel indexModelForSelectedSchemaNode = null;
		ElasticsearchIndexSchemaFieldNode selectedSchemaNode = null;

		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			ElasticsearchIndexSchemaFieldNode schemaNode = indexModel.getFieldNode( absoluteFieldPath );
			if ( schemaNode != null ) {
				if ( selectedSchemaNode == null ) {
					selectedSchemaNode = schemaNode;
					indexModelForSelectedSchemaNode = indexModel;
				}
				else if ( !selectedSchemaNode.isCompatibleWith( schemaNode ) ) {
					throw log.conflictingFieldTypesForSearch(
							absoluteFieldPath,
							selectedSchemaNode, indexModelForSelectedSchemaNode.getIndexName(),
							schemaNode, indexModel.getIndexName() );
				}
			}
		}
		if ( selectedSchemaNode == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexNames() );
		}
		return selectedSchemaNode;
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
