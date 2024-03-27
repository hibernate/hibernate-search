/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.backend.lucene.type.asnative;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

import org.apache.lucene.document.FeatureField;
import org.apache.lucene.document.StoredField;

//tag::include[]
public class PageRankValueBinder implements ValueBinder { // <1>
	@Override
	public void bind(ValueBindingContext<?> context) {
		context.bridge(
				Float.class,
				new PageRankValueBridge(),
				context.typeFactory() // <2>
						.extension( LuceneExtension.get() ) // <3>
						.asNative( // <4>
								Float.class, // <5>
								(absoluteFieldPath, value, collector) -> { // <6>
									collector.accept( new FeatureField( absoluteFieldPath, "pageRank", value ) );
									collector.accept( new StoredField( absoluteFieldPath, value ) );
								},
								field -> (Float) field.numericValue() // <7>
						)
		);
	}

	private static class PageRankValueBridge implements ValueBridge<Float, Float> {
		@Override
		public Float toIndexedValue(Float value, ValueBridgeToIndexedValueContext context) {
			return value; // <8>
		}

		@Override
		public Float fromIndexedValue(Float value, ValueBridgeFromIndexedValueContext context) {
			return value; // <8>
		}
	}
}
//end::include[]
