/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;

public final class CustomMarkerConsumingPropertyBridge implements PropertyBridge {

	private List<IndexObjectFieldAccessor> objectFieldAccessors = new ArrayList<>();

	@Override
	public void bind(PropertyBridgeBindingContext context) {
		List<PojoModelProperty> markedProperties = context.getBridgedElement().properties()
				.filter( property -> property.markers( CustomMarker.class ).findAny().isPresent() )
				.collect( Collectors.toList() );
		for ( PojoModelProperty property : markedProperties ) {
			objectFieldAccessors.add(
					context.getIndexSchemaElement().objectField( property.getName() ).createAccessor()
			);
		}
	}

	@Override
	public void write(DocumentElement target, PojoElement source) {
		for ( IndexObjectFieldAccessor objectFieldAccessor : objectFieldAccessors ) {
			objectFieldAccessor.add( target );
		}
	}
}
