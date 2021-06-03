/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.search.impl.AbstractLuceneSearchCompositeIndexSchemaElementQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchCompositeIndexSchemaElementContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexSchemaElementContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSingleFieldPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

public class LuceneObjectExistsPredicate extends AbstractLuceneSingleFieldPredicate {

	private final List<LuceneSearchPredicate> children;

	private LuceneObjectExistsPredicate(Builder builder) {
		super( builder );
		children = builder.children;
		// Ensure illegal attempts to mutate the predicate will fail
		builder.children = null;
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		// if exists at least one not-null field, exists on object field should match
		BooleanQuery.Builder builder = new BooleanQuery.Builder();

		for ( LuceneSearchPredicate child : children ) {
			builder.add( child.toQuery( context ), BooleanClause.Occur.SHOULD );
		}

		/*
		 * When executing an exists() predicate on an object field that contains dynamic value field,
		 * we don't necessarily know all the possible (dynamic) child value fields,
		 * so we cannot just execute {@code exists(childField1) OR exists(childField2) OR ... OR exists(childFieldN)}.
		 * That's why we keep track of the fact that
		 * "for this document, this object field exists because it contains at least one dynamic value"
		 * by adding the path of the object field to the "fieldNames" field.
		 */
		builder.add( new TermQuery( new Term( MetadataFields.fieldNamesFieldName(), absoluteFieldPath ) ),
				BooleanClause.Occur.SHOULD );

		return builder.build();
	}

	public static class Factory extends
			AbstractLuceneSearchCompositeIndexSchemaElementQueryElementFactory<ExistsPredicateBuilder> {
		public static final Factory INSTANCE = new Factory();

		private Factory() {
		}

		@Override
		public ExistsPredicateBuilder create(LuceneSearchIndexScope scope, LuceneSearchCompositeIndexSchemaElementContext field) {
			Builder builder = new Builder( scope, field );
			for ( LuceneSearchIndexSchemaElementContext child : field.staticChildrenByName().values() ) {
				builder.addChild( child );
			}
			return builder;
		}
	}

	private static class Builder extends AbstractBuilder implements ExistsPredicateBuilder {
		private List<LuceneSearchPredicate> children = new ArrayList<>();

		public Builder(LuceneSearchIndexScope scope, LuceneSearchCompositeIndexSchemaElementContext field) {
			super( scope, field );
		}

		public void addChild(LuceneSearchIndexSchemaElementContext child) {
			if ( child.isComposite() && child.toComposite().nested() ) {
				// TODO HSEARCH-3904 Elasticsearch ignores children that are nested object fields.
				//  We align on that behavior... for now.
				return;
			}
			children.add( LuceneSearchPredicate.from( scope,
					child.queryElement( PredicateTypeKeys.EXISTS, scope ).build() ) );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneObjectExistsPredicate( this );
		}
	}
}
