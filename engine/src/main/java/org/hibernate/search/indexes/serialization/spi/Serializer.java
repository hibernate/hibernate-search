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
package org.hibernate.search.indexes.serialization.spi;

import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Fieldable;

import org.hibernate.search.backend.LuceneWork;

/**
 * Contract between Hibernate Search and the Serialization mechanism.
 * Step in building the specific structures are represented by a method.
 *
 * LuceneWorkSerializer controls the LuceneWork traversal flow.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface Serializer {

	void luceneWorks(List<LuceneWork> works);

	void addOptimizeAll();

	void addPurgeAll(String entityClassName);

	void addIdSerializedInJava(byte[] id);

	void addIdAsInteger(int id);

	void addIdAsLong(long id);

	void addIdAsFloat(float id);

	void addIdAsDouble(double id);

	void addIdAsString(String id);

	void addDelete(String entityClassName);

	void addAdd(String entityClassName, Map<String, String> fieldToAnalyzerMap);

	void addUpdate(String entityClassName, Map<String, String> fieldToAnalyzerMap);

	byte[] serialize();

	void fields(List<Fieldable> fields);

	void addIntNumericField(int value, LuceneNumericFieldContext context);

	void addLongNumericField(long value, LuceneNumericFieldContext context);

	void addFloatNumericField(float value, LuceneNumericFieldContext context);

	void addDoubleNumericField(double value, LuceneNumericFieldContext context);

	void addFieldWithBinaryData(LuceneFieldContext luceneFieldContext);

	void addFieldWithStringData(LuceneFieldContext luceneFieldContext);

	void addFieldWithTokenStreamData(LuceneFieldContext luceneFieldContext);

	void addFieldWithSerializableReaderData(LuceneFieldContext luceneFieldContext);

	void addFieldWithSerializableFieldable(byte[] fieldable);

	void addDocument(float boost);
}
