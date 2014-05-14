/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import org.hibernate.search.indexes.serialization.spi.LuceneFieldContext;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public abstract class SerializableField implements SerializableFieldable {
	private String name;
	//TODO state not kept, how to know if a string ought to be interned? default seems to be intern
	//private boolean interned
	private float boost;
	private boolean omitNorms;
	private boolean omitTermFreqAndPositions;

	public SerializableField(LuceneFieldContext context) {
		this.name = context.getName();
		this.boost = context.getBoost();
		this.omitNorms = context.isOmitNorms();
		this.omitTermFreqAndPositions = context.isOmitTermFreqAndPositions();
	}

	public String getName() {
		return name;
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
