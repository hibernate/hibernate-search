/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.spi;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.lucene.util.AttributeImpl;
import org.hibernate.search.backend.spi.DeletionQuery;
import org.hibernate.search.bridge.spi.ConversionContext;

/**
 * During de-serialization a {@code Deserializer} needs to build a list of {@code LuceneWork} instances from
 * binary data. To do so, it gets passed a {@code LuceneWorksBuilder} instance which defines "template
 * methods" to assemble the work instances.
 * <p>
 * Note:<br>
 * The order in which calls need to be made is not clearly defined making this API rather fragile. Refer to
 * {@code AvroDeserializer} to see how the builder is used.
 * </p>
 *
 * @see Deserializer
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface LuceneWorksBuilder {
	void addOptimizeAll();

	void addFlush();

	void addPurgeAllLuceneWork(String entityClassName);

	void addIdAsJavaSerialized(byte[] idAsByte);

	void addId(Serializable id);

	void addDeleteLuceneWork(String entityClassName, ConversionContext conversionContext);

	void addDeleteByQueryLuceneWork(String entityClassName, DeletionQuery deletionQuery);

	void addAddLuceneWork(String entityClassName, Map<String, String> fieldToAnalyzerMap, ConversionContext conversionContext);

	void addUpdateLuceneWork(String entityClassName, Map<String, String> fieldToAnalyzerMap, ConversionContext conversionContext);

	void defineDocument();

	void addFieldable(byte[] instance);

	void addIntNumericField(int value, String name, int precisionStep, SerializableStore store, boolean indexed, float boost, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addLongNumericField(long value, String name, int precisionStep, SerializableStore store, boolean indexed, float boost, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addFloatNumericField(float value, String name, int precisionStep, SerializableStore store, boolean indexed, float boost, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addDoubleNumericField(double value, String name, int precisionStep, SerializableStore store, boolean indexed, float boost, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addFieldWithBinaryData(String name, byte[] value, int offset, int length);

	void addFieldWithStringData(String name, String value, SerializableStore store, SerializableIndex index, SerializableTermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addFieldWithTokenStreamData(String name, SerializableTermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addFieldWithSerializableReaderData(String name, byte[] value, SerializableTermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addSerializedAttribute(byte[] bytes);

	void addAttributeInstance(AttributeImpl attribute);

	void addTokenTrackingAttribute(List<Integer> positions);

	void addCharTermAttribute(CharSequence sequence);

	void addPayloadAttribute(byte[] payloads);

	void addKeywordAttribute(boolean isKeyword);

	void addPositionIncrementAttribute(int positionIncrement);

	void addFlagsAttribute(int flags);

	void addTypeAttribute(String type);

	void addOffsetAttribute(int startOffset, int endOffset);

	void addToken();

	void addDocValuesFieldWithBinaryData(String name, String type, byte[] value, int offset, int length);

	void addDocValuesFieldWithNumericData(String name, String type, long value);
}
