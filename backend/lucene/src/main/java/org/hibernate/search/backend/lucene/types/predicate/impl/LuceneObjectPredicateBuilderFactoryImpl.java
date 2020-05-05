/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.document.model.impl.AbstractLuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;

public class LuceneObjectPredicateBuilderFactoryImpl implements LuceneObjectPredicateBuilderFactory {

	private final String absoluteFieldPath;
	private final List<String> nestedPathHierarchy;
	private final Map<String, LuceneFieldPredicateBuilderFactory> leafFields = new HashMap<>();

	public LuceneObjectPredicateBuilderFactoryImpl(LuceneIndexSchemaObjectFieldNode objectNode) {
		absoluteFieldPath = objectNode.absolutePath();
		nestedPathHierarchy = objectNode.getNestedPathHierarchy();
		addLeafFields( objectNode );
	}

	@Override
	public boolean isCompatibleWith(LuceneObjectPredicateBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}

		LuceneObjectPredicateBuilderFactoryImpl casted = (LuceneObjectPredicateBuilderFactoryImpl) other;
		Set<String> leafFieldPaths = leafFields.keySet();
		if ( !leafFieldPaths.equals( casted.leafFields.keySet() ) ) {
			return false;
		}

		for ( String leafFieldPath : leafFieldPaths ) {
			if ( !leafFields.get( leafFieldPath ).hasCompatibleCodec( casted.leafFields.get( leafFieldPath ) ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ExistsPredicateBuilder<LuceneSearchPredicateBuilder> createExistsPredicateBuilder() {
		LuceneExistsCompositePredicateBuilder objectPredicateBuilder = new LuceneExistsCompositePredicateBuilder(
				absoluteFieldPath, nestedPathHierarchy
		);
		for ( Map.Entry<String, LuceneFieldPredicateBuilderFactory> entry : leafFields.entrySet() ) {
			ExistsPredicateBuilder<LuceneSearchPredicateBuilder> existsPredicateBuilder = entry.getValue().createExistsPredicateBuilder( entry.getKey(), nestedPathHierarchy );
			objectPredicateBuilder.addChild( existsPredicateBuilder );
		}
		return objectPredicateBuilder;
	}

	private void addLeafFields(LuceneIndexSchemaObjectFieldNode objectNode) {
		for ( AbstractLuceneIndexSchemaFieldNode child : objectNode.staticChildren() ) {
			if ( child.isObjectField() && !child.toObjectField().type().isNested() ) {
				// add recursively flattened nested object fields: this is the ES behavior
				addLeafFields( child.toObjectField() );
			}
			else if ( child.isValueField() ) {
				leafFields.put(
						child.absolutePath(),
						( (LuceneIndexSchemaFieldNode<?>) child ).type().getPredicateBuilderFactory()
				);
			}
		}
	}
}
