/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.document.FieldSelector;

import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.query.QueryHits;

/**
 * Helper class to extract <code>EntityInfo</code>s out of the <code>QueryHits</code>.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DocumentExtractor {
	private final SearchFactoryImplementor searchFactoryImplementor;
	private final String[] projection;
	private final QueryHits queryHits;
	private FieldSelector fieldSelector;
	private boolean allowFieldSelection;
	private boolean needId;
	private final Map<String,Class> targetedClasses;
	private final Class singleClassIfPossible;
	
	@Deprecated
	public DocumentExtractor(QueryHits queryHits, SearchFactoryImplementor searchFactoryImplementor, String[] projection, Set<String> idFieldNames, boolean allowFieldSelection) {
		this(queryHits, searchFactoryImplementor, projection, idFieldNames, allowFieldSelection, Collections.EMPTY_SET);
	}

	public DocumentExtractor(QueryHits queryHits, SearchFactoryImplementor searchFactoryImplementor, String[] projection, Set<String> idFieldNames, boolean allowFieldSelection, Set<Class<?>> classesAndSubclasses) {
		this.searchFactoryImplementor = searchFactoryImplementor;
		if ( projection != null ) {
			this.projection = projection.clone();
		}
		else {
			this.projection = null;
		}
		this.queryHits = queryHits;
		this.allowFieldSelection = allowFieldSelection;
		this.targetedClasses = new HashMap<String,Class>( classesAndSubclasses.size() );
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
				else if ( ProjectionConstants.BOOST.equals( projectionName ) ) {
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
				else {
					fields.put( projectionName, FieldSelectorResult.LOAD );
				}
			}
		}
		if ( singleClassIfPossible == null ) {
			fields.put( DocumentBuilder.CLASS_FIELDNAME, FieldSelectorResult.LOAD );
		}
		if ( needId ) {
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
		this.fieldSelector = new MapFieldSelector( fields );
	}

	private EntityInfo extract(Document document) {
		Class clazz = extractClass( document );
		String idName = DocumentBuilderHelper.getDocumentIdName( searchFactoryImplementor, clazz );
		Serializable id = needId ? DocumentBuilderHelper.getDocumentId( searchFactoryImplementor, clazz, document ) : null;
		Object[] projected = null;
		if ( projection != null && projection.length > 0 ) {
			projected = DocumentBuilderHelper.getDocumentFields(
					searchFactoryImplementor, clazz, document, projection
			);
		}
		return new EntityInfo( clazz, idName, id, projected );
	}

	private Class extractClass(Document document) {
		//maybe we can avoid document extraction:
		if ( singleClassIfPossible != null ) {
			return singleClassIfPossible;
		}
		String className = document.get( DocumentBuilder.CLASS_FIELDNAME );
		Class clazz = targetedClasses.get( className );
		//and maybe we can avoid the Reflect helper:
		if ( clazz != null ) {
			return clazz;
		}
		else {
			return DocumentBuilderHelper.getDocumentClass( className );
		}
	}

	public EntityInfo extract(int index) throws IOException {
		Document doc;
		if ( allowFieldSelection ) {
			doc = queryHits.doc( index, fieldSelector );
		}
		else {
			doc = queryHits.doc( index );
		}

		EntityInfo entityInfo = extract( doc );
		Object[] eip = entityInfo.projection;

		// TODO - if we are only looking for score (unlikely), avoid accessing doc (lazy load)
		if ( eip != null && eip.length > 0 ) {
			for ( int x = 0; x < projection.length; x++ ) {
				if ( ProjectionConstants.SCORE.equals( projection[x] ) ) {
					eip[x] = queryHits.score( index );
				}
				else if ( ProjectionConstants.ID.equals( projection[x] ) ) {
					eip[x] = entityInfo.id;
				}
				else if ( ProjectionConstants.DOCUMENT.equals( projection[x] ) ) {
					eip[x] = doc;
				}
				else if ( ProjectionConstants.DOCUMENT_ID.equals( projection[x] ) ) {
					eip[x] = queryHits.docId( index );
				}
				else if ( ProjectionConstants.BOOST.equals( projection[x] ) ) {
					eip[x] = doc.getBoost();
				}
				else if ( ProjectionConstants.EXPLANATION.equals( projection[x] ) ) {
					eip[x] = queryHits.explain( index );
				}
				else if ( ProjectionConstants.OBJECT_CLASS.equals( projection[x] ) ) {
						eip[x] = entityInfo.clazz;
				}
				else if ( ProjectionConstants.THIS.equals( projection[x] ) ) {
					//THIS could be projected more than once
					//THIS loading delayed to the Loader phase
					entityInfo.indexesOfThis.add( x );
				}
			}
		}
		return entityInfo;
	}
	
}
