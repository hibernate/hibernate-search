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
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.util.common.SearchException;

public class LuceneObjectPredicateBuilderFactoryImpl implements LuceneObjectPredicateBuilderFactory {

	private final String absoluteFieldPath;
	private final List<String> nestedPathHierarchy;
	private final Map<String, LuceneSearchValueFieldContext<?>> leafFields = new HashMap<>();

	public LuceneObjectPredicateBuilderFactoryImpl(LuceneIndexSchemaObjectFieldNode objectNode) {
		absoluteFieldPath = objectNode.absolutePath();
		nestedPathHierarchy = objectNode.nestedPathHierarchy();
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
			// TODO temporary hack - we should use query element factories for object fields as well
			try {
				leafFields.get( leafFieldPath ).type().queryElementFactory( PredicateTypeKeys.EXISTS )
						.checkCompatibleWith( casted.leafFields.get( leafFieldPath ).type()
								.queryElementFactory( PredicateTypeKeys.EXISTS ) );
				return true;
			}
			catch (SearchException e) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ExistsPredicateBuilder createExistsPredicateBuilder(LuceneSearchContext searchContext) {
		LuceneObjectExistsPredicate.Builder builder = new LuceneObjectExistsPredicate.Builder(
				searchContext, absoluteFieldPath, nestedPathHierarchy
		);
		for ( Map.Entry<String, LuceneSearchValueFieldContext<?>> entry : leafFields.entrySet() ) {
			ExistsPredicateBuilder existsPredicateBuilder =
					entry.getValue().queryElement( PredicateTypeKeys.EXISTS, searchContext );
			builder.addChild( existsPredicateBuilder.build() );
		}
		return builder;
	}

	private void addLeafFields(LuceneIndexSchemaObjectFieldNode objectNode) {
		for ( AbstractLuceneIndexSchemaFieldNode child : objectNode.staticChildren() ) {
			if ( child.isObjectField() && !child.toObjectField().type().nested() ) {
				// add recursively flattened nested object fields: this is the ES behavior
				addLeafFields( child.toObjectField() );
			}
			else if ( child.isValueField() ) {
				leafFields.put( child.absolutePath(), (LuceneSearchValueFieldContext<?>) child );
			}
		}
	}
}
