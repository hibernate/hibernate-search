/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.query.engine.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.search.TopDocs;

import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.impl.DocumentBuilderHelper;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
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

	private final SearchFactoryImplementor searchFactoryImplementor;
	private final String[] projection;
	private final QueryHits queryHits;
	private final IndexSearcherWithPayload searcher;
	private FieldSelector fieldSelector;
	private boolean allowFieldSelection;
	private boolean needId;
	private final Map<String, Class> targetedClasses;
	private int firstIndex;
	private int maxIndex;
	private Object query;
	private final Class singleClassIfPossible; //null when not possible
	private final FieldCacheCollector classTypeCollector; //null when not used
	private final FieldCacheCollector idsCollector; //null when not used
	private final ConversionContext exceptionWrap = new ContextualExceptionBridgeHelper();

	public DocumentExtractorImpl(QueryHits queryHits,
								SearchFactoryImplementor searchFactoryImplementor,
								String[] projection,
								Set<String> idFieldNames,
								boolean allowFieldSelection,
								IndexSearcherWithPayload searcher,
								Object query,
								int firstIndex,
								int maxIndex,
								Set<Class<?>> classesAndSubclasses) {
		this.searchFactoryImplementor = searchFactoryImplementor;
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
		Map<String, FieldSelectorResult> fields;
		if ( projection == null ) {
			// we're going to load hibernate entities
			needId = true;
			fields = new HashMap<String, FieldSelectorResult>( 2 ); // id + class
		}
		else {
			fields = new HashMap<String, FieldSelectorResult>( projection.length + 2 ); // we actually have no clue
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
					fields.put( projectionName, FieldSelectorResult.LOAD );
				}
			}
		}
		if ( singleClassIfPossible == null && classTypeCollector == null ) {
			fields.put( ProjectionConstants.OBJECT_CLASS, FieldSelectorResult.LOAD );
		}
		if ( needId && idsCollector == null ) {
			for ( String idFieldName : idFieldNames ) {
				fields.put( idFieldName, FieldSelectorResult.LOAD );
			}
		}
		if ( fields.size() == 1 ) {
			// surprised: from unit tests it seems this case is possible quite often
			// so apply an additional optimization using LOAD_AND_BREAK instead:
			String key = fields.keySet().iterator().next();
			fields.put( key, FieldSelectorResult.LOAD_AND_BREAK );
		}
		if ( fields.size() != 0 ) {
			this.fieldSelector = new MapFieldSelector( fields );
		}
		// else: this.fieldSelector = null; //We need no fields at all
	}

	private EntityInfo extractEntityInfo(int docId, Document document, int scoreDocIndex, ConversionContext exceptionWrap) throws IOException {
		Class clazz = extractClass( docId, document, scoreDocIndex );
		String idName = DocumentBuilderHelper.getDocumentIdName( searchFactoryImplementor, clazz );
		Serializable id = extractId( docId, document, clazz );
		Object[] projected = null;
		if ( projection != null && projection.length > 0 ) {
			projected = DocumentBuilderHelper.getDocumentFields(
					searchFactoryImplementor, clazz, document, projection, exceptionWrap
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
			return DocumentBuilderHelper.getDocumentId( searchFactoryImplementor, clazz, document, exceptionWrap );
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
			return DocumentBuilderHelper.getDocumentClass( className );
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
		searcher.closeSearcher( query, searchFactoryImplementor );
	}

	private Document extractDocument(int index) throws IOException {
		if ( allowFieldSelection ) {
			if ( fieldSelector == null ) {
				//we need no fields
				return null;
			}
			else {
				return queryHits.doc( index, fieldSelector );
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
		Map<String, FieldSelectorResult> fields = new HashMap<String, FieldSelectorResult>( 1 );
		fields.put( ProjectionConstants.OBJECT_CLASS, FieldSelectorResult.LOAD_AND_BREAK );
		MapFieldSelector classOnly = new MapFieldSelector( fields );
		Document doc = queryHits.doc( scoreDocIndex, classOnly );
		return doc.get( ProjectionConstants.OBJECT_CLASS );
	}

	@Override
	public TopDocs getTopDocs() {
		return queryHits.getTopDocs();
	}

}
