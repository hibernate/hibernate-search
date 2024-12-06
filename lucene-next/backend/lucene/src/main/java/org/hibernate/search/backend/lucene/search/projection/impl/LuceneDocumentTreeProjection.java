/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexCompositeNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexNode;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.StoredFieldsValuesDelegate;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.ChildDocIds;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.reporting.impl.LuceneSearchHints;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeIndexManagerContext;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneSearchIndexScopeImpl;
import org.hibernate.search.backend.lucene.search.projection.dsl.DocumentTree;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;

class LuceneDocumentTreeProjection extends AbstractLuceneProjection<DocumentTree>
		implements LuceneSearchProjection.Extractor<DocumentTree, DocumentTree> {

	private final Set<String> nestedObjectsPaths;
	private final List<LuceneIndexModel> models;

	LuceneDocumentTreeProjection(LuceneSearchIndexScopeImpl scope) {
		super( scope );
		nestedObjectsPaths = new LinkedHashSet<>();
		models = new ArrayList<>();
		for ( LuceneScopeIndexManagerContext index : scope.indexes() ) {
			var model = index.model();
			models.add( model );
			if ( model.hasNestedDocuments() ) {
				for ( IndexFieldDescriptor field : model.root().staticChildren() ) {
					collect( nestedObjectsPaths, field );
				}
			}
		}
	}

	private static void collect(Set<String> nestedPaths, IndexFieldDescriptor field) {
		if ( field.isObjectField() ) {
			if ( field.toObjectField().type().nested() ) {
				nestedPaths.add( field.absolutePath() );
			}
			for ( IndexFieldDescriptor child : field.toObjectField().staticChildren() ) {
				collect( nestedPaths, child );
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, DocumentTree> request(ProjectionRequestContext context) {
		context.checkNotNested(
				LuceneProjectionTypeKeys.DOCUMENT,
				LuceneSearchHints.INSTANCE.documentProjectionNestingNotSupportedHint()
		);
		context.requireAllStoredFields();
		context.requireNestedObjects( nestedObjectsPaths );
		return this;
	}

	@Override
	public Values<DocumentTree> values(ProjectionExtractContext context) {
		List<ChildDocumentTreeValues> children = new ArrayList<>();
		for ( LuceneIndexModel model : models ) {
			children.addAll( createChildrenDocumentTrees( context, model.root() ) );
		}
		return new RootDocumentTreeValues( context.collectorExecutionContext().storedFieldsValuesDelegate(), children );
	}

	private List<ChildDocumentTreeValues> createChildrenDocumentTrees(ProjectionExtractContext context,
			LuceneIndexCompositeNode node) {
		List<ChildDocumentTreeValues> result = new ArrayList<>();
		for ( LuceneIndexNode child : node.staticChildren() ) {
			if ( child.isObjectField() && child.toObjectField().type().nested() ) {
				result.add( new ChildDocumentTreeValues(
						context.collectorExecutionContext(),
						node.nestedDocumentPath(),
						child.absolutePath(),
						createChildrenDocumentTrees( context, child.toObjectField() )
				) );
			}
		}
		return result;
	}

	@Override
	public DocumentTree transform(LoadingResult<?> loadingResult, DocumentTree extractedData,
			ProjectionTransformContext context) {
		return extractedData;
	}

	private static class RootDocumentTreeValues implements Values<DocumentTree> {

		private final StoredFieldsValuesDelegate storedFieldsValuesDelegate;
		private final List<ChildDocumentTreeValues> children;

		private RootDocumentTreeValues(StoredFieldsValuesDelegate storedFieldsValuesDelegate,
				List<ChildDocumentTreeValues> children) {
			this.storedFieldsValuesDelegate = storedFieldsValuesDelegate;
			this.children = children;
		}

		@Override
		public void context(LeafReaderContext context) throws IOException {
			for ( ChildDocumentTreeValues child : children ) {
				child.context( context );
			}
		}

		@Override
		public DocumentTree get(int doc) throws IOException {
			Map<String, List<DocumentTree>> nested = new LinkedHashMap<>();
			for ( ChildDocumentTreeValues child : children ) {
				List<DocumentTree> nodes = child.get( doc );
				if ( !nodes.isEmpty() ) {
					nested.put( child.getPath(), nodes );
				}
			}
			return new DocumentTreeImpl( storedFieldsValuesDelegate.get( doc ), Collections.unmodifiableMap( nested ) );
		}
	}

	private static class ChildDocumentTreeValues implements Values<List<DocumentTree>> {
		private final StoredFieldsValuesDelegate storedFieldsValuesDelegate;
		private final String path;
		private final NestedDocsProvider nestedDocsProvider;
		private final List<ChildDocumentTreeValues> children;

		private ChildDocIds childDocIds;

		public ChildDocumentTreeValues(TopDocsDataCollectorExecutionContext context, String parent, String path,
				List<ChildDocumentTreeValues> children) {
			this.storedFieldsValuesDelegate = context.storedFieldsValuesDelegate();
			this.path = path;
			this.children = children;
			this.nestedDocsProvider = context.createNestedDocsProvider( parent, path );
		}

		@Override
		public void context(LeafReaderContext context) throws IOException {
			childDocIds = nestedDocsProvider.childDocs( context, null );
			for ( ChildDocumentTreeValues child : children ) {
				child.context( context );
			}
		}

		@Override
		public List<DocumentTree> get(int doc) throws IOException {
			List<DocumentTree> result = new ArrayList<>();
			if ( childDocIds != null && childDocIds.advanceExactParent( doc ) ) {
				for ( int currentChildDocId = childDocIds.nextChild();
						currentChildDocId != DocIdSetIterator.NO_MORE_DOCS;
						currentChildDocId = childDocIds.nextChild() ) {

					Map<String, List<DocumentTree>> nested = new LinkedHashMap<>();
					for ( ChildDocumentTreeValues child : children ) {
						List<DocumentTree> nodes = child.get( currentChildDocId );
						if ( !nodes.isEmpty() ) {
							nested.put( child.getPath().substring( path.length() + 1, child.path.length() ), nodes );
						}
					}

					result.add( new DocumentTreeImpl( storedFieldsValuesDelegate.get( currentChildDocId ),
							Collections.unmodifiableMap( nested ) ) );
				}
			}

			return Collections.unmodifiableList( result );
		}

		public String getPath() {
			return path;
		}
	}

	private static class DocumentTreeImpl implements DocumentTree {

		private final Document document;
		private final Map<String, Collection<DocumentTree>> nested;

		private DocumentTreeImpl(Document document, Map<String, Collection<DocumentTree>> nested) {
			this.document = document;
			this.nested = nested;
		}

		@Override
		public Document document() {
			return document;
		}

		@Override
		public Map<String, Collection<DocumentTree>> nested() {
			return nested;
		}

		@Override
		public String toString() {
			return "DocumentTree{" +
					"document=" + document +
					", nested=" + nested +
					'}';
		}
	}
}
