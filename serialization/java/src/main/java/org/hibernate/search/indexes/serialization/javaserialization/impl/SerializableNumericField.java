/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import org.hibernate.search.indexes.serialization.spi.LuceneNumericFieldContext;
import org.hibernate.search.indexes.serialization.spi.SerializableStore;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public abstract class SerializableNumericField implements SerializableFieldable {

	private String name;
	private int precisionStep;
	private SerializableStore store;
	private boolean indexed; //or should it be Index for future extension?
	private float boost;
	private boolean omitNorms;
	private boolean omitTermFreqAndPositions;

	public SerializableNumericField(LuceneNumericFieldContext context) {
		this.name = context.getName();
		this.precisionStep = context.getPrecisionStep();
		this.store = context.getStore();
		this.indexed = context.isIndexed();
		this.boost = context.getBoost();
		this.omitNorms = context.getOmitNorms();
		this.omitTermFreqAndPositions = context.getOmitTermFreqAndPositions();
	}

	public String getName() {
		return name;
	}

	public int getPrecisionStep() {
		return precisionStep;
	}

	public SerializableStore getStore() {
		return store;
	}

	public boolean isIndexed() {
		return indexed;
	}

	public float getBoost() {
		return boost;
	}

	public boolean isOmitNorms() {
		return omitNorms;
	}

	public boolean isOmitTermFreqAndPositions() {
		return omitTermFreqAndPositions;
	}
}
