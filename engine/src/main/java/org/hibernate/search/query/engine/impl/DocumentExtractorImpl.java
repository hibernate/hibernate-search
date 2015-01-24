/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.impl.DocumentBuilderHelper;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.collector.impl.FieldCacheCollector;

import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;


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
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DocumentExtractorImpl implements DocumentExtractor {

	private static final Log log = LoggerFactory.make();

	private final ExtendedSearchIntegrator extendedIntegrator;
	private final String[] projection;
	private final QueryHits queryHits;
	private final LazyQueryState searcher;
	private ReusableDocumentStoredFieldVisitor fieldLoadingVisitor;
	private boolean allowFieldSelection;
	private boolean needId;
	private final Map<String, Class> targetedClasses;
	private int firstIndex;
	private int maxIndex;
	private Query query;
	private final Class singleClassIfPossible; //null when not possible
	private final FieldCacheCollector classTypeCollector; //null when not used
	private final FieldCacheCollector idsCollector; //null when not used
	private final ConversionContext exceptionWrap = new ContextualExceptionBridgeHelper();

	public DocumentExtractorImpl(QueryHits queryHits,
								ExtendedSearchIntegrator extendedIntegrator,
								String[] projection,
								Set<String> idFieldNames,
								boolean allowFieldSelection,
								LazyQueryState searcher,
								Query query,
								int firstIndex,
								int maxIndex,
								Set<Class<?>> classesAndSubclasses) {
		this.extendedIntegrator = extendedIntegrator;
		if ( projection != null ) {
			this.projection = projection.clone();
		}
		else {
			this.projection = null;
		}
		this.queryHits = queryHits;
		this.allowFieldSelection = allowFieldSelection;
		this.targetedClasses = new HashMap<String, Class>( classesAndSubclasses.size() );
		for ( Class<?> clazz : classesAndSubclasses ) {
			//useful to reload classes from index without using reflection
			targetedClasses.put( clazz.getName(), clazz );
		}
		if ( classesAndSubclasses.size() == 1 ) {
			singleClassIfPossible = classesAndSubclasses.iterator().next();
		}
		else {
			singleClassIfPossible = null;
		}
		this.searcher = searcher;
		this.query = query;
		this.firstIndex = firstIndex;
		this.maxIndex = maxIndex;
		this.classTypeCollector = queryHits.getClassTypeCollector();
		this.idsCollector = queryHits.getIdsCollector();
		initFieldSelection( projection, idFieldNames );
	}

	private void initFieldSelection(String[] projection, Set<String> idFieldNames) {
		HashSet<String> fields;
		if ( projection == null ) {
			// we're going to load hibernate entities
			needId = true;
			fields = new HashSet<String>( 2 ); // id + class
		}
		else {
			fields = new HashSet<String>( projection.length + 2 ); // we actually have no clue
			for ( String projectionName : projection ) {
				if ( projectionName == null ) {
					continue;
				}
				else if ( ProjectionConstants.THIS.equals( projectionName ) ) {
					needId = true;
				}
				else if ( ProjectionConstants.DOCUMENT.equals( projectionName ) ) {
					// if we need to project DOCUMENT do not use fieldSelector as the user might want anything
					allowFieldSelection = false;
					needId = true;
					return;
				}
				else if ( ProjectionConstants.SCORE.equals( projectionName ) ) {
					continue;
				}
				else if ( ProjectionConstants.ID.equals( projectionName ) ) {
					needId = true;
				}
				else if ( ProjectionConstants.DOCUMENT_ID.equals( projectionName ) ) {
					continue;
				}
				else if ( ProjectionConstants.EXPLANATION.equals( projectionName ) ) {
					continue;
				}
				else if ( ProjectionConstants.OBJECT_CLASS.equals( projectionName ) ) {
					continue;
				}
				else if ( ProjectionConstants.SPATIAL_DISTANCE.equals( projectionName ) ) {
					continue;
				}
				else {
					fields.add( projectionName );
				}
			}
		}
		if ( singleClassIfPossible == null && classTypeCollector == null ) {
			fields.add( ProjectionConstants.OBJECT_CLASS );
		}
		if ( needId && idsCollector == null ) {
			for ( String idFieldName : idFieldNames ) {
				fields.add( idFieldName );
			}
		}
		if ( fields.size() != 0 ) {
			this.fieldLoadingVisitor = new ReusableDocumentStoredFieldVisitor( fields );
		}
		// else: this.fieldSelector = null; //We need no fields at all
	}

	private EntityInfo extractEntityInfo(int docId, Document document, int scoreDocIndex, ConversionContext exceptionWrap) throws IOException {
		Class clazz = extractClass( docId, document, scoreDocIndex );
		String idName = DocumentBuilderHelper.getDocumentIdName( extendedIntegrator, clazz );
		Serializable id = extractId( docId, document, clazz );
		Object[] projected = null;
		if ( projection != null && projection.length > 0 ) {
			projected = DocumentBuilderHelper.getDocumentFields(
					extendedIntegrator, clazz, document, projection, exceptionWrap
			);
		}
		return new EntityInfoImpl( clazz, idName, id, projected );
	}

	private Serializable extractId(int docId, Document document, Class clazz) {
		if ( !needId ) {
			return null;
		}
		else if ( this.idsCollector != null ) {
			return (Serializable) this.idsCollector.getValue( docId );
		}
		else {
			return DocumentBuilderHelper.getDocumentId( extendedIntegrator, clazz, document, exceptionWrap );
		}
	}

	private Class extractClass(int docId, Document document, int scoreDocIndex) throws IOException {
		//maybe we can avoid document extraction:
		if ( singleClassIfPossible != null ) {
			return singleClassIfPossible;
		}
		String className;
		if ( classTypeCollector != null ) {
			className = (String) classTypeCollector.getValue( docId );
			if ( className == null ) {
				log.forceToUseDocumentExtraction();
				className = forceClassNameExtraction( scoreDocIndex );
			}
		}
		else {
			className = document.get( ProjectionConstants.OBJECT_CLASS );
		}
		//and quite likely we can avoid the Reflect helper:
		Class clazz = targetedClasses.get( className );
		if ( clazz != null ) {
			return clazz;
		}
		else {
			return DocumentBuilderHelper.getDocumentClass( className, extendedIntegrator.getServiceManager() );
		}
	}

	@Override
	public EntityInfo extract(int scoreDocIndex) throws IOException {
		int docId = queryHits.docId( scoreDocIndex );
		Document document = extractDocument( scoreDocIndex );

		EntityInfo entityInfo = extractEntityInfo( docId, document, scoreDocIndex, exceptionWrap );
		Object[] eip = entityInfo.getProjection();

		if ( eip != null && eip.length > 0 ) {
			for ( int x = 0; x < projection.length; x++ ) {
				if ( ProjectionConstants.SCORE.equals( projection[x] ) ) {
					eip[x] = queryHits.score( scoreDocIndex );
				}
				else if ( ProjectionConstants.ID.equals( projection[x] ) ) {
					eip[x] = entityInfo.getId();
				}
				else if ( ProjectionConstants.DOCUMENT.equals( projection[x] ) ) {
					eip[x] = document;
				}
				else if ( ProjectionConstants.DOCUMENT_ID.equals( projection[x] ) ) {
					eip[x] = docId;
				}
				else if ( ProjectionConstants.EXPLANATION.equals( projection[x] ) ) {
					eip[x] = queryHits.explain( scoreDocIndex );
				}
				else if ( ProjectionConstants.OBJECT_CLASS.equals( projection[x] ) ) {
					eip[x] = entityInfo.getClazz();
				}
				else if ( ProjectionConstants.SPATIAL_DISTANCE.equals( projection[x] ) ) {
					eip[x] = queryHits.spatialDistance( scoreDocIndex );
				}
				else if ( ProjectionConstants.THIS.equals( projection[x] ) ) {
					//THIS could be projected more than once
					//THIS loading delayed to the Loader phase
					entityInfo.getIndexesOfThis().add( x );
				}
			}
		}
		return entityInfo;
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
	 * In rare cases the Lucene {@code FieldCache} might fail to return a value, at this point we already extracted
	 * the {@code Document} so we need to repeat the process to extract the missing field only.
	 *
	 * @param scoreDocIndex the document index
	 *
	 * @return the index class name stored in the {@code ProjectionConstants.OBJECT_CLASS} field.
	 *
	 * @throws IOException
	 */
	private String forceClassNameExtraction(int scoreDocIndex) throws IOException {
		final Set<String> fields = Collections.singleton( ProjectionConstants.OBJECT_CLASS );
		final ReusableDocumentStoredFieldVisitor classOnly = new ReusableDocumentStoredFieldVisitor( fields );
		queryHits.visitDocument( scoreDocIndex, classOnly );
		final Document doc = classOnly.getDocumentAndReset();
		return doc.get( ProjectionConstants.OBJECT_CLASS );
	}

	/**
	 * Required by Infinispan Query.
	 */
	public TopDocs getTopDocs() {
		return queryHits.getTopDocs();
	}

}
