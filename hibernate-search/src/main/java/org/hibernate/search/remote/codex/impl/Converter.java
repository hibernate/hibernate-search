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
package org.hibernate.search.remote.codex.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.remote.operations.impl.Add;
import org.hibernate.search.remote.operations.impl.Delete;
import org.hibernate.search.remote.operations.impl.Index;
import org.hibernate.search.remote.operations.impl.SerializableBinaryField;
import org.hibernate.search.remote.operations.impl.SerializableDocument;
import org.hibernate.search.remote.operations.impl.SerializableDoubleField;
import org.hibernate.search.remote.operations.impl.LuceneFieldContext;
import org.hibernate.search.remote.operations.impl.SerializableField;
import org.hibernate.search.remote.operations.impl.SerializableFieldable;
import org.hibernate.search.remote.operations.impl.SerializableFloatField;
import org.hibernate.search.remote.operations.impl.SerializableIntField;
import org.hibernate.search.remote.operations.impl.SerializableLongField;
import org.hibernate.search.remote.operations.impl.LuceneNumericFieldContext;
import org.hibernate.search.remote.operations.impl.Message;
import org.hibernate.search.remote.operations.impl.Operation;
import org.hibernate.search.remote.operations.impl.OptimizeAll;
import org.hibernate.search.remote.operations.impl.PurgeAll;
import org.hibernate.search.remote.operations.impl.SerializableCustomFieldable;
import org.hibernate.search.remote.operations.impl.SerializableNumericField;
import org.hibernate.search.remote.operations.impl.SerializableReaderField;
import org.hibernate.search.remote.operations.impl.SerializableStringField;
import org.hibernate.search.remote.operations.impl.SerializableTokenStreamField;
import org.hibernate.search.remote.operations.impl.Store;
import org.hibernate.search.remote.operations.impl.TermVector;
import org.hibernate.search.remote.operations.impl.Update;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class Converter {
	public static Message toSerializedModel(List<LuceneWork> works) {
		Set<Operation> ops = new HashSet<Operation>( works.size() );
		for (LuceneWork work : works) {
			if (work instanceof OptimizeLuceneWork) {
				ops.add( new OptimizeAll() );
			}
			else if (work instanceof PurgeAllLuceneWork) {
				PurgeAllLuceneWork safeWork = (PurgeAllLuceneWork) work;
				ops.add( new PurgeAll( safeWork.getEntityClass() ) );
			}
			else if (work instanceof DeleteLuceneWork) {
				DeleteLuceneWork safeWork = (DeleteLuceneWork) work;
				ops.add( new Delete( safeWork.getEntityClass(), safeWork.getId() ) );
			}
			else if (work instanceof AddLuceneWork ) {
				AddLuceneWork safeWork = (AddLuceneWork) work;
				SerializableDocument document = buildDocument( safeWork.getDocument() );
				ops.add( new Add( work.getEntityClass(), work.getId(), document, work.getFieldToAnalyzerMap() ) );
			}
			else if (work instanceof UpdateLuceneWork ) {
				UpdateLuceneWork safeWork = (UpdateLuceneWork) work;
				SerializableDocument document = buildDocument( safeWork.getDocument() );
				ops.add( new Update( work.getEntityClass(), work.getId(), document, work.getFieldToAnalyzerMap() ) );
			}
		}
		return new Message(1,ops);
	}

	public static List<LuceneWork> toLuceneWorks(Message message, SearchFactoryImplementor searchFactory) {
		if (message.getProtocolVersion() != 1) {
			throw new SearchException( "Serialization protocol not supported. Protocol version: " + message.getProtocolVersion() );
		}
		List<LuceneWork> results = new ArrayList<LuceneWork>( message.getOperations().size() );
		for ( Operation operation : message.getOperations() ) {
			if (operation instanceof OptimizeAll) {
				results.add( new OptimizeLuceneWork() );
			}
			else if (operation instanceof PurgeAll) {
				PurgeAll safeOperation = (PurgeAll) operation;
				results.add( new PurgeAllLuceneWork( safeOperation.getEntityClass() ) );
			}
			else if (operation instanceof Delete) {
				Delete safeOperation = (Delete) operation;
				LuceneWork result = new DeleteLuceneWork(
						safeOperation.getId(),
						objectIdInString(safeOperation, searchFactory),
						safeOperation.getEntityClass()
				);
				results.add( result );
			}
			else if (operation instanceof Add) {
				Add safeOperation = (Add) operation;
				Document document = buildLuceneDocument( safeOperation.getDocument() );
				LuceneWork result = new AddLuceneWork(
						safeOperation.getId(),
						objectIdInString(safeOperation, searchFactory),
						safeOperation.getEntityClass(),
						document,
						safeOperation.getFieldToAnalyzerMap()
				);
				results.add( result );
			}
			else if (operation instanceof Update) {
				Update safeOperation = (Update) operation;
				Document document = buildLuceneDocument( safeOperation.getDocument() );
				LuceneWork result = new AddLuceneWork(
						safeOperation.getId(),
						objectIdInString(safeOperation, searchFactory),
						safeOperation.getEntityClass(),
						document,
						safeOperation.getFieldToAnalyzerMap()
				);
				results.add( result );
			}
		}
		return results;
	}

	private static Document buildLuceneDocument(SerializableDocument document) {
		Document luceneDocument = new Document();
		luceneDocument.setBoost( document.getBoost() );
		for (SerializableFieldable field : document.getFieldables() ) {
			if (field instanceof SerializableCustomFieldable) {
				SerializableCustomFieldable safeField = (SerializableCustomFieldable) field;
				luceneDocument.add( (Fieldable) safeField.getInstance() );
			}
			else if (field instanceof SerializableNumericField ) {
				SerializableNumericField safeField = (SerializableNumericField) field;
				NumericField numField = new NumericField(
						safeField.getName(),
						safeField.getPrecisionStep(),
						getStore( safeField.getStore() ),
						safeField.isIndexed() );
				numField.setOmitNorms( safeField.isOmitNorms() );
				numField.setOmitTermFreqAndPositions( safeField.isOmitTermFreqAndPositions() );
				if ( field instanceof SerializableIntField ) {
					numField.setIntValue( ( (SerializableIntField) field ).getValue() );
				}
				else if ( field instanceof SerializableLongField ) {
					numField.setLongValue( ( ( SerializableLongField ) field ).getValue() );
				}
				else if ( field instanceof SerializableFloatField ) {
					numField.setFloatValue( ( ( SerializableFloatField ) field ).getValue() );
				}
				else if ( field instanceof SerializableDoubleField ) {
					numField.setDoubleValue( ( ( SerializableDoubleField ) field ).getValue() );
				}
				else {
					throw new SearchException( "Unknown SerializableNumericField: " + field.getClass() );
				}
				luceneDocument.add( numField );
			}
			else if (field instanceof SerializableField) {
				SerializableField safeField = (SerializableField) field;
				Field luceneField;
				if ( field instanceof SerializableBinaryField ) {
					SerializableBinaryField reallySafeField = (SerializableBinaryField) field;
					luceneField = new Field(
							reallySafeField.getName(),
							reallySafeField.getValue(),
							reallySafeField.getOffset(),
							reallySafeField.getLength()
							);
					//no store no index no term vector
				}
				else if ( field instanceof SerializableStringField ) {
					SerializableStringField reallySafeField = (SerializableStringField) field;
					luceneField = new Field(
							reallySafeField.getName(),
							reallySafeField.getValue(),
							getStore( reallySafeField.getStore() ),
							getIndex( reallySafeField.getIndex() ),
							getTermVector( reallySafeField.getTermVector() )
							);
				}
				else if ( field instanceof SerializableTokenStreamField ) {
					SerializableTokenStreamField reallySafeField = (SerializableTokenStreamField) field;
					//no store no index
					luceneField = new Field(
							reallySafeField.getName(),
							new CopyTokenStream( reallySafeField.getValue() ),
							getTermVector( reallySafeField.getTermVector() )
							);
				}
				else if ( field instanceof SerializableReaderField ) {
					SerializableReaderField reallySafeField = (SerializableReaderField) field;
					//no store no index
					luceneField = new Field(
							reallySafeField.getName(),
							reallySafeField.getValue(),
							getTermVector( reallySafeField.getTermVector() )
							);
				}
				else {
					throw new SearchException( "Unknown SerializableField: " + field.getClass() );
				}

				luceneField.setBoost( safeField.getBoost() );
				luceneField.setOmitNorms( safeField.isOmitNorms() );
				luceneField.setOmitTermFreqAndPositions( safeField.isOmitTermFreqAndPositions() );
				luceneDocument.add( luceneField );
			}
			else {
				throw new SearchException( "Unknown SerializableFieldable: " + field.getClass() );
			}
		}
		return luceneDocument;
	}

	private static Field.TermVector getTermVector(TermVector termVector) {
		switch ( termVector ) {
			case NO:
				return Field.TermVector.NO;
			case WITH_OFFSETS:
				return Field.TermVector.WITH_OFFSETS;
			case WITH_POSITIONS:
				return Field.TermVector.WITH_POSITIONS;
			case WITH_POSITIONS_OFFSETS:
				return Field.TermVector.WITH_POSITIONS_OFFSETS;
			case YES:
				return Field.TermVector.YES;
			 default:
				throw new SearchException( "Unable to convert serializable TermVector to Lucene TermVector: " + termVector );
		}
	}

	private static Field.Index getIndex(Index index) {
		switch ( index ) {
			case ANALYZED:
				return Field.Index.ANALYZED;
			case ANALYZED_NO_NORMS:
				return Field.Index.ANALYZED_NO_NORMS;
			case NO:
				return Field.Index.NO;
			case NOT_ANALYZED:
				return Field.Index.NOT_ANALYZED;
			case NOT_ANALYZED_NO_NORMS:
				return Field.Index.NOT_ANALYZED_NO_NORMS;
			default:
				throw new SearchException( "Unable to convert serializable Index to Lucene Index: " + index );
		}
	}

	private static Field.Store getStore(Store store) {
		switch ( store ) {
			case NO:
				return Field.Store.NO;
			case YES:
				return Field.Store.YES;
			default:
				throw new SearchException( "Unable to convert serializable Store to Lucene Store: " + store );
		}
	}

	private static String objectIdInString(Delete safeOperation, SearchFactoryImplementor searchFactory) {
		Class<?> entityClass = safeOperation.getEntityClass();
		DocumentBuilderIndexedEntity<?> documentBuilder = searchFactory.getIndexBindingForEntity( entityClass )
				.getDocumentBuilder();
		return documentBuilder.objectToString( documentBuilder.getIdKeywordName(), safeOperation.getId() );
	}

	private static String objectIdInString(Add safeOperation, SearchFactoryImplementor searchFactory) {
		Class<?> entityClass = safeOperation.getEntityClass();
		DocumentBuilderIndexedEntity<?> documentBuilder = searchFactory.getIndexBindingForEntity( entityClass )
				.getDocumentBuilder();
		return documentBuilder.objectToString( documentBuilder.getIdKeywordName(), safeOperation.getId() );
	}

	private static String objectIdInString(Update safeOperation, SearchFactoryImplementor searchFactory) {
		Class<?> entityClass = safeOperation.getEntityClass();
		DocumentBuilderIndexedEntity<?> documentBuilder = searchFactory.getIndexBindingForEntity( entityClass )
				.getDocumentBuilder();
		return documentBuilder.objectToString( documentBuilder.getIdKeywordName(), safeOperation.getId() );
	}

	private static SerializableDocument buildDocument(Document document) {
		List<Fieldable> docFields = document.getFields();
		Set<SerializableFieldable> serialFields = new HashSet<SerializableFieldable>( docFields.size() );
		for(Fieldable fieldable : docFields) {
			SerializableFieldable serialFieldable;
			if (fieldable instanceof NumericField) {
				NumericField safeField = (NumericField) fieldable;
				LuceneNumericFieldContext context = new LuceneNumericFieldContext( (NumericField) fieldable );
				switch ( safeField.getDataType() ) {
					case INT:
						serialFieldable = new SerializableIntField(
								safeField.getNumericValue().intValue(),
								context
						);
						break;
					case LONG:
						serialFieldable = new SerializableLongField(
								safeField.getNumericValue().longValue(),
								context
						);
						break;
					case FLOAT:
						serialFieldable = new SerializableFloatField(
								safeField.getNumericValue().floatValue(),
								context
						);
						break;
					case DOUBLE:
						serialFieldable = new SerializableDoubleField(
								safeField.getNumericValue().doubleValue(),
								context
						);
						break;
					default:
					    throw new SearchException( "Unknown NumericField type: " + safeField.getDataType() );
				}
			}
			else if (fieldable instanceof Field) {
				Field safeField = (Field) fieldable;
				if ( safeField.isBinary() ) {
					serialFieldable = new SerializableBinaryField( new LuceneFieldContext(safeField) );
				}
				else if ( safeField.stringValue() != null )  {
					serialFieldable = new SerializableStringField( new LuceneFieldContext(safeField) );
				}
				else if ( safeField.readerValue() != null && safeField.readerValue() instanceof Serializable )  {
					serialFieldable = new SerializableReaderField( new LuceneFieldContext(safeField) );
				}
				else if ( safeField.readerValue() != null )  {
					throw new SearchException( "Conversion from Reader to String not yet implemented" );
				}
				else if ( safeField.tokenStreamValue() != null )  {
					serialFieldable = new SerializableTokenStreamField( new LuceneFieldContext(safeField) );
				}
				else {
					throw new SearchException( "Unknown value type for Field: " + safeField );
				}
			}
			else if (fieldable instanceof Serializable) {
				serialFieldable = new SerializableCustomFieldable( (Serializable) fieldable );
			}
			else {
				throw new SearchException( "Cannot serialize custom field '" + fieldable.getClass() + "'. Must be NumericField, Field or a Serializable Fieldable implementation." );
			}
			serialFields.add( serialFieldable );
		}
		return new SerializableDocument( serialFields, document.getBoost() );
	}
}
