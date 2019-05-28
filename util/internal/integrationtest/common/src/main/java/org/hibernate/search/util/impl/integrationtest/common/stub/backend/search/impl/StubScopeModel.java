/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.converter.impl.StubFieldConverter;

public class StubScopeModel {
	private final List<String> indexNames;
	private final List<StubIndexSchemaNode> rootSchemaNodes;

	public StubScopeModel(List<String> indexNames, List<StubIndexSchemaNode> rootSchemaNodes) {
		this.indexNames = indexNames;
		this.rootSchemaNodes = rootSchemaNodes;
	}

	public List<String> getIndexNames() {
		return indexNames;
	}

	public StubFieldConverter<?> getFieldConverter(String absoluteFieldPath) {
		String[] pathComponents = absoluteFieldPath.split( "\\." );
		List<StubIndexSchemaNode> matchingNodes = new ArrayList<>();
		for ( StubIndexSchemaNode rootSchemaNode : rootSchemaNodes ) {
			matchingNodes.addAll( getSchemaNodes( rootSchemaNode, pathComponents ) );
		}
		if ( matchingNodes.isEmpty() ) {
			throw new IllegalStateException( "Reference to unknown field. There is a problem in tests" );
		}

		StubFieldConverter<?> result = null;
		for ( StubIndexSchemaNode matchingNode : matchingNodes ) {
			StubFieldConverter<?> converter = matchingNode.getConverter();
			if ( result == null ) {
				result = converter;
			}
			else if ( !result.isConvertIndexToProjectionCompatibleWith( converter ) ) {
				throw new IllegalStateException(
						"Reference to field '" + absoluteFieldPath
								+ "' on indexes " + indexNames
								+ " with multiple, incompatible definitions"
								+ " (probably different type or projections converters)."
				);
			}
		}
		return result;
	}

	private static List<StubIndexSchemaNode> getSchemaNodes(StubIndexSchemaNode root, String[] pathComponents) {
		List<StubIndexSchemaNode> parents;
		List<StubIndexSchemaNode> children = CollectionHelper.asList( root );
		for ( String pathComponent : pathComponents ) {
			parents = children;
			children = new ArrayList<>();
			for ( StubIndexSchemaNode parent : parents ) {
				List<StubIndexSchemaNode> childrenToAdd = parent.getChildren().get( pathComponent );
				if ( childrenToAdd != null ) {
					children.addAll( childrenToAdd );
				}
			}
		}
		return children;
	}
}
