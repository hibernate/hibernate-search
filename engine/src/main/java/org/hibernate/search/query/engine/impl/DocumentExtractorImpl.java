/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.impl.DocumentBuilderHelper;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.spi.IndexedTypeIdentifier;


/**
 * DocumentExtractor is a traverser over the full-text results (EntityInfo)
 *
 * This operation is as lazy as possible:
 * - the query is executed eagerly
 * - results are not retrieved until actually requested
 *
 * #getFirstIndex and #getMaxIndex define the boundaries available to #extract.
 *
 * DocumentExtractor objects *must* be closed when the results are no longer traversed.
 * #close
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class DocumentExtractorImpl implements DocumentExtractor {

	private final ExtendedSearchIntegrator extendedIntegrator;
	private final String[] projection;
	private final QueryHits queryHits;
	private final LazyQueryState searcher;
	private ReusableDocumentStoredFieldVisitor fieldLoadingVisitor;
	private boolean allowFieldSelection;
	private boolean needId;
	private boolean hasProjectionConstants;
	private final Map<String, EntityIndexBinding> targetedEntityBindings;
	private final int firstIndex;
	private final int maxIndex;
	private final EntityIndexBinding singleEntityBindingIfPossible; //null when not possible
	private final ConversionContext exceptionWrap = new ContextualExceptionBridgeHelper();

	public DocumentExtractorImpl(QueryHits queryHits,
								ExtendedSearchIntegrator extendedIntegrator,
								String[] projection,
								Set<String> idFieldNames,
								boolean allowFieldSelection,
								LazyQueryState searcher,
								int firstIndex,
								int maxIndex,
								Map<String, EntityIndexBinding> targetedEntityBindings) {
		this.extendedIntegrator = extendedIntegrator;
		if ( projection != null ) {
			this.projection = projection.clone();
		}
		else {
			this.projection = null;
		}
		this.queryHits = queryHits;
		this.allowFieldSelection = allowFieldSelection;
		if ( targetedEntityBindings.size() == 1 ) {
			this.singleEntityBindingIfPossible = targetedEntityBindings.values().iterator().next();
			this.targetedEntityBindings = null;
		}
		else {
			this.singleEntityBindingIfPossible = null;
			this.targetedEntityBindings = targetedEntityBindings;
		}
		this.searcher = searcher;
		this.firstIndex = firstIndex;
		this.maxIndex = maxIndex;
		initFieldSelection( projection, idFieldNames );
	}

	private void initFieldSelection(String[] projection, Set<String> idFieldNames) {
		HashSet<String> fields;
		if ( projection == null ) {
			// we're going to load hibernate entities
			needId = true;
			fields = new HashSet<>( 2 ); // id + class
		}
		else {
			fields = new HashSet<>( projection.length + 2 ); // we actually have no clue
			for ( String projectionName : projection ) {
				if ( null == projectionName ) {
					continue;
				}
				else switch (projectionName) {
			    	case ProjectionConstants.THIS:
				    needId = true;
				    hasProjectionConstants = true;
				    break;
			    	case ProjectionConstants.DOCUMENT:
				    // if we need to project DOCUMENT do not use fieldSelector as the user might want anything
				    allowFieldSelection = false;
				    needId = true;
				    hasProjectionConstants = true;
				    return;
			    	case ProjectionConstants.SCORE:
				    hasProjectionConstants = true;
				    break;
			    	case ProjectionConstants.ID:
				    needId = true;
				    hasProjectionConstants = true;
				    break;
			    	case ProjectionConstants.DOCUMENT_ID:
				    hasProjectionConstants = true;
				    break;
			    	case ProjectionConstants.EXPLANATION:
				    hasProjectionConstants = true;
				    break;
			    	case ProjectionConstants.OBJECT_CLASS:
				    hasProjectionConstants = true;
				    break;
			    	case ProjectionConstants.SPATIAL_DISTANCE:
				    hasProjectionConstants = true;
				    break;
			    	default:
				    fields.add( projectionName );
				    break;
			    }
			}
		}
		if ( singleEntityBindingIfPossible == null ) {
			fields.add( ProjectionConstants.OBJECT_CLASS );
		}
		if ( needId ) {
			for ( String idFieldName : idFieldNames ) {
				fields.add( idFieldName );
			}
		}
		if ( fields.size() != 0 ) {
			this.fieldLoadingVisitor = new ReusableDocumentStoredFieldVisitor( fields );
		}
		// else: this.fieldSelector = null; //We need no fields at all
	}

	private Serializable extractId(DocumentBuilderIndexedEntity documentBuilder, Document document) {
		if ( !needId ) {
			return null;
		}
		else {
			return DocumentBuilderHelper.getDocumentId( documentBuilder, document, exceptionWrap );
		}
	}

	private DocumentBuilderIndexedEntity extractDocumentBuilder(Document document) throws IOException {
		//maybe we can avoid document extraction:
		if ( singleEntityBindingIfPossible != null ) {
			return singleEntityBindingIfPossible.getDocumentBuilder();
		}
		String className = document.get( ProjectionConstants.OBJECT_CLASS );

		//and quite likely we can avoid the Reflect helper:
		EntityIndexBinding entityBinding = targetedEntityBindings.get( className );
		if ( entityBinding != null ) {
			return entityBinding.getDocumentBuilder();
		}

		return DocumentBuilderHelper.getDocumentBuilder( className, extendedIntegrator );
	}

	@Override
	public EntityInfo extract(int scoreDocIndex) throws IOException {
		int docId = queryHits.docId( scoreDocIndex );
		Document document = extractDocument( scoreDocIndex );

		DocumentBuilderIndexedEntity documentBuilder = extractDocumentBuilder( document );
		IndexedTypeIdentifier type = documentBuilder.getTypeIdentifier();
		String idName = documentBuilder.getIdPropertyName();
		Serializable id = extractId( documentBuilder, document );
		Object[] projected = null;
		if ( projection != null && projection.length > 0 ) {
			projected = DocumentBuilderHelper.getDocumentFields(
					documentBuilder, document, projection, exceptionWrap
			);
			if ( hasProjectionConstants ) {
				for ( int x = 0; x < projection.length; x++ ) {
					if ( null != projection[x] ) switch (projection[x]) {
				    	case ProjectionConstants.SCORE:
					    projected[x] = queryHits.score( scoreDocIndex );
					    break;
				    	case ProjectionConstants.ID:
					    projected[x] = id;
					    break;
				    	case ProjectionConstants.DOCUMENT:
					    projected[x] = document;
					    break;
				    	case ProjectionConstants.DOCUMENT_ID:
					    projected[x] = docId;
					    break;
				    	case ProjectionConstants.EXPLANATION:
					    projected[x] = queryHits.explain( scoreDocIndex );
					    break;
				    	case ProjectionConstants.OBJECT_CLASS:
					    projected[x] = type.getPojoType();
					    break;
				    	case ProjectionConstants.SPATIAL_DISTANCE:
					    projected[x] = queryHits.spatialDistance( scoreDocIndex );
					    break;
				    	case ProjectionConstants.THIS:
					    //THIS could be projected more than once
					    //THIS loading delayed to the Loader phase
					    // Use EntityInfo.ENTITY_PLACEHOLDER as placeholder.
					    // It will be replaced when we populate
					    // the EntityInfo with the real entity.
					    projected[x] = EntityInfo.ENTITY_PLACEHOLDER;
					    break;
				    	default:
					    break;
				    }
				}
			}
		}
		return new EntityInfoImpl( type, idName, id, projected );
	}

	@Override
	public int getFirstIndex() {
		return firstIndex;
	}

	@Override
	public int getMaxIndex() {
		return maxIndex;
	}

	@Override
	public void close() {
		searcher.close();
	}

	private Document extractDocument(int index) throws IOException {
		if ( allowFieldSelection ) {
			if ( fieldLoadingVisitor == null ) {
				//we need no fields
				return null;
			}
			else {
				queryHits.visitDocument( index, fieldLoadingVisitor );
				return fieldLoadingVisitor.getDocumentAndReset();
			}
		}
		else {
			return queryHits.doc( index );
		}
	}

	/**
	 * Required by Infinispan Query.
	 */
	@Override
	public TopDocs getTopDocs() {
		return queryHits.getTopDocs();
	}

}
