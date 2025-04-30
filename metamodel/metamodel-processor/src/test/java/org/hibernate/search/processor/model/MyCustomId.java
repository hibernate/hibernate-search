/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.model;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

public record MyCustomId(long id, String string) {

	public class MyCustomIdBinder implements IdentifierBinder {

		@Override
		public void bind(IdentifierBindingContext<?> context) {
			context.bridge(
					MyCustomId.class,
					new Bridge()
			);
		}

		public static class Bridge implements IdentifierBridge<MyCustomId> {
			@Override
			public String toDocumentIdentifier(MyCustomId value,
					IdentifierBridgeToDocumentIdentifierContext context) {
				return value.id() + "/" + value.string();
			}

			@Override
			public MyCustomId fromDocumentIdentifier(String documentIdentifier,
					IdentifierBridgeFromDocumentIdentifierContext context) {
				String[] split = documentIdentifier.split( "/" );
				return new MyCustomId( Long.parseLong( split[0] ), split[1] );
			}
		}
	}

}
