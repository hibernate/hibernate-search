/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import java.util.List;

import org.apache.lucene.util.AttributeImpl;

import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.serialization.impl.SerializationHelper;
import org.hibernate.search.indexes.serialization.spi.Deserializer;
import org.hibernate.search.indexes.serialization.spi.LuceneWorksBuilder;

/**
 * A work deserializer which uses Java's native serialization mechanism.
 *
 * Note:
 * <p>
 * Since the underlying Lucene classes are not serializable, we need to create serializable wrapper classes
 * for all Lucene classes which we need to transfer. These wrapper classes get populated during serialization and then
 * used at de-serialization to re-create the appropriate Lucene classes.
 * </p>
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Hardy Ferentschik
 */
public class NativeJavaDeserializer implements Deserializer {

	@Override
	public void deserialize(byte[] data, LuceneWorksBuilder luceneWorksBuilder) {
		Message message = SerializationHelper.toInstance( data, Message.class );
		final ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
		for ( Operation operation : message.getOperations() ) {
			if ( operation instanceof OptimizeAll ) {
				luceneWorksBuilder.addOptimizeAll();
			}
			else if ( operation instanceof PurgeAll ) {
				PurgeAll safeOperation = (PurgeAll) operation;
				luceneWorksBuilder.addPurgeAllLuceneWork( safeOperation.getEntityClassName() );
			}
			else if ( operation instanceof Flush ) {
				luceneWorksBuilder.addFlush();
			}
			else if ( operation instanceof Delete ) {
				Delete safeOperation = (Delete) operation;
				if ( safeOperation.getId() instanceof byte[] ) {
					luceneWorksBuilder.addIdAsJavaSerialized( (byte[])safeOperation.getId() );
				}
				else {
					luceneWorksBuilder.addId( safeOperation.getId() );
				}
				luceneWorksBuilder.addDeleteLuceneWork(
						safeOperation.getEntityClassName(),
						conversionContext
				);
			}
			else if ( operation instanceof Add ) {
				Add safeOperation = (Add) operation;
				buildLuceneDocument( safeOperation.getDocument(), luceneWorksBuilder );
				luceneWorksBuilder.addId( safeOperation.getId() );
				luceneWorksBuilder.addAddLuceneWork(
						safeOperation.getEntityClassName(),
						safeOperation.getFieldToAnalyzerMap(),
						conversionContext
				);
			}
			else if ( operation instanceof Update ) {
				Update safeOperation = (Update) operation;
				buildLuceneDocument( safeOperation.getDocument(), luceneWorksBuilder );
				luceneWorksBuilder.addId( safeOperation.getId() );
				luceneWorksBuilder.addUpdateLuceneWork(
						safeOperation.getEntityClassName(),
						safeOperation.getFieldToAnalyzerMap(),
						conversionContext
				);
			}
		}
	}

	private void buildLuceneDocument(SerializableDocument document, LuceneWorksBuilder hydrator) {
		hydrator.defineDocument();
		for ( SerializableFieldable field : document.getFieldables() ) {
			if ( field instanceof SerializableCustomFieldable ) {
				SerializableCustomFieldable safeField = (SerializableCustomFieldable) field;
				hydrator.addFieldable( safeField.getInstance() );
			}
			else if ( field instanceof SerializableNumericField ) {
				SerializableNumericField safeField = (SerializableNumericField) field;
				if ( field instanceof SerializableIntField ) {
					hydrator.addIntNumericField(
							( (SerializableIntField) field ).getValue(),
							safeField.getName(),
							safeField.getPrecisionStep(),
							safeField.getStore(),
							safeField.isIndexed(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableLongField ) {
					hydrator.addLongNumericField(
							( (SerializableLongField) field ).getValue(),
							safeField.getName(),
							safeField.getPrecisionStep(),
							safeField.getStore(),
							safeField.isIndexed(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableFloatField ) {
					hydrator.addFloatNumericField(
							( (SerializableFloatField) field ).getValue(),
							safeField.getName(),
							safeField.getPrecisionStep(),
							safeField.getStore(),
							safeField.isIndexed(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableDoubleField ) {
					hydrator.addDoubleNumericField(
							( (SerializableDoubleField) field ).getValue(),
							safeField.getName(),
							safeField.getPrecisionStep(),
							safeField.getStore(),
							safeField.isIndexed(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else {
					throw new SearchException( "Unknown SerializableNumericField: " + field.getClass() );
				}
			}
			else if ( field instanceof SerializableField ) {
				SerializableField safeField = (SerializableField) field;
				if ( field instanceof SerializableBinaryField ) {
					SerializableBinaryField reallySafeField = (SerializableBinaryField) field;
					hydrator.addFieldWithBinaryData(
							reallySafeField.getName(),
							reallySafeField.getValue(),
							reallySafeField.getOffset(),
							reallySafeField.getLength()
					);
				}
				else if ( field instanceof SerializableStringField ) {
					SerializableStringField reallySafeField = (SerializableStringField) field;
					hydrator.addFieldWithStringData(
							reallySafeField.getName(),
							reallySafeField.getValue(),
							reallySafeField.getStore(),
							reallySafeField.getIndex(),
							reallySafeField.getTermVector(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableTokenStreamField ) {
					SerializableTokenStreamField reallySafeField = (SerializableTokenStreamField) field;
					List<List<AttributeImpl>> tokens = reallySafeField.getValue().getStream();
					for ( List<AttributeImpl> token : tokens ) {
						for ( AttributeImpl attribute : token ) {
							hydrator.addAttributeInstance( attribute );
						}
						hydrator.addToken();
					}
					hydrator.addFieldWithTokenStreamData(
							reallySafeField.getName(),
							reallySafeField.getTermVector(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableReaderField ) {
					SerializableReaderField reallySafeField = (SerializableReaderField) field;
					hydrator.addFieldWithSerializableReaderData(
							reallySafeField.getName(),
							reallySafeField.getValue(),
							reallySafeField.getTermVector(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else {
					throw new SearchException( "Unknown SerializableField: " + field.getClass() );
				}
			}
			else {
				throw new SearchException( "Unknown SerializableFieldable: " + field.getClass() );
			}
		}
	}
}
