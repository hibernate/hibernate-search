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
package org.hibernate.search.remote.codex.spi;

import java.util.List;
import java.util.Map;

import org.apache.lucene.util.AttributeImpl;

import org.hibernate.search.remote.operations.impl.Index;
import org.hibernate.search.remote.operations.impl.Store;
import org.hibernate.search.remote.operations.impl.TermVector;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface LuceneHydrator {
	void addOptimizeAll();

	void addPurgeAllLuceneWork(String entityClassName);

	void addDeleteLuceneWork(String entityClassName, byte[] id);

	void addAddLuceneWork(String entityClassName, byte[] id, Map<String, String> fieldToAnalyzerMap);

	void addUpdateLuceneWork(String entityClassName, byte[] id, Map<String, String> fieldToAnalyzerMap);

	void defineDocument(float boost);

	void addFieldable(byte[] instance);

	//TODO forgot boost => do it across the whole chain
	void addIntNumericField(int value, String name, int precisionStep, Store store, boolean indexed, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addLongNumericField(long value, String name, int precisionStep, Store store, boolean indexed, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addFloatNumericField(float value, String name, int precisionStep, Store store, boolean indexed, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addDoubleNumericField(double value, String name, int precisionStep, Store store, boolean indexed, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addFieldWithBinaryData(String name, byte[] value, int offset, int length, float boost, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addFieldWithStringData(String name, String value, Store store, Index index, TermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addFieldWithTokenStreamData(String name, List<List<AttributeImpl>> tokenStream, TermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions);

	void addFieldWithSerializableReaderData(String name, byte[] value, TermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions);
}
