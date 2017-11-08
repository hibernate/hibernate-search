/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementType;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

/**
 * @author Yoann Rodiere
 */
class IntegerFieldModelContext extends AbstractScalarFieldModelContext<Integer> {

	private final UnknownTypeJsonAccessor accessor;

	public IntegerFieldModelContext(UnknownTypeJsonAccessor accessor) {
		this.accessor = accessor;
	}

	@Override
	protected PropertyMapping contribute(DeferredInitializationIndexFieldReference<Integer> reference, ElasticsearchFieldModelCollector collector) {
		PropertyMapping mapping = super.contribute( reference, collector );

		ElasticsearchFieldFormatter formatter = IntegerFieldFormatter.INSTANCE;
		reference.initialize( new ElasticsearchIndexFieldReference<>( accessor, formatter ) );
		mapping.setType( DataType.INTEGER );

		String absolutePath = accessor.getStaticAbsolutePath();
		ElasticsearchFieldModel model = new ElasticsearchFieldModel( formatter );
		collector.collect( absolutePath, model );

		return mapping;
	}

	private static final class IntegerFieldFormatter implements ElasticsearchFieldFormatter {
		// Must be a singleton so that equals() works as required by the interface
		public static final IntegerFieldFormatter INSTANCE = new IntegerFieldFormatter();

		private IntegerFieldFormatter() {
		}

		@Override
		public JsonElement format(Object object) {
			if ( object == null ) {
				return JsonNull.INSTANCE;
			}
			Integer value = (Integer) object;
			return new JsonPrimitive( value );
		}

		@Override
		public Object parse(JsonElement element) {
			if ( element == null || element.isJsonNull() ) {
				return null;
			}
			return JsonElementType.INTEGER.fromElement( element );
		}
	}
}
