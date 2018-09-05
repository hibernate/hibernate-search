/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.spi;

import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.DeletionQuery;

/**
 * Contract between Hibernate Search and the Serialization mechanism.
 * Step in building the specific structures are represented by a method.
 *
 * LuceneWorkSerializer controls the LuceneWork traversal flow.
 *
 * @author Emmanuel Bernard
 */
public interface Serializer {

	void luceneWorks(List<LuceneWork> works);

	void addOptimizeAll();

	void addFlush();

	void addPurgeAll(String entityClassName);

	void addIdSerializedInJava(byte[] id);

	void addIdAsInteger(int id);

	void addIdAsLong(long id);

	void addIdAsFloat(float id);

	void addIdAsDouble(double id);

	void addIdAsString(String id);

	void addDelete(String entityClassName);

	void addDeleteByQuery(String entityClassName, DeletionQuery deletionQuery);

	void addAdd(String entityClassName, Map<String, String> fieldToAnalyzerMap);

	void addUpdate(String entityClassName, Map<String, String> fieldToAnalyzerMap);

	byte[] serialize();

	void fields(List<IndexableField> fields);

	void addIntNumericField(int value, LuceneNumericFieldContext context);

	void addLongNumericField(long value, LuceneNumericFieldContext context);

	void addFloatNumericField(float value, LuceneNumericFieldContext context);

	void addDoubleNumericField(double value, LuceneNumericFieldContext context);

	void addFieldWithBinaryData(LuceneFieldContext luceneFieldContext);

	void addFieldWithStringData(LuceneFieldContext luceneFieldContext);

	void addFieldWithTokenStreamData(LuceneFieldContext luceneFieldContext);

	void addFieldWithSerializableReaderData(LuceneFieldContext luceneFieldContext);

	void addFieldWithSerializableFieldable(byte[] fieldable);

	void addDocValuesFieldWithBinaryValue(LuceneFieldContext luceneFieldContext);

	void addDocValuesFieldWithNumericValue(long value, LuceneFieldContext luceneFieldContext);

	void addDocument();

}
