/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

public enum BackendIndexingOperation {

	ADD {
		@Override
		void expect(BackendMock.DocumentWorkCallListContext context, Object tenantId,
				String id, String routingKey, String value, String containedValue) {
			context.add( b -> addWorkInfoAndDocument( b, tenantId, id, routingKey, value, containedValue ) );
		}
	},
	ADD_OR_UPDATE {
		@Override
		void expect(BackendMock.DocumentWorkCallListContext context, Object tenantId,
				String id, String routingKey, String value, String containedValue) {
			context.addOrUpdate( b -> addWorkInfoAndDocument( b, tenantId, id, routingKey, value, containedValue ) );
		}
	},
	DELETE {
		@Override
		void expect(BackendMock.DocumentWorkCallListContext context, Object tenantId,
				String id, String routingKey, String value, String containedValue) {
			context.delete( b -> addWorkInfo( b, tenantId, id, routingKey ) );
		}
	};

	abstract void expect(BackendMock.DocumentWorkCallListContext context, Object tenantId,
			String id, String routingKey, String value, String containedValue);

	static void addWorkInfo(StubDocumentWork.Builder builder, Object tenantId,
			String identifier, String routingKey) {
		builder.tenantIdentifier( tenantId );
		builder.identifier( identifier );
		builder.routingKey( routingKey );
	}

	static void addWorkInfoAndDocument(StubDocumentWork.Builder builder, Object tenantId,
			String identifier, String routingKey, String value, String containedValue) {
		builder.tenantIdentifier( tenantId );
		builder.identifier( identifier );
		builder.routingKey( routingKey );
		StubDocumentNode.Builder documentBuilder = StubDocumentNode.document();
		documentBuilder.field( "value", value );
		if ( containedValue != null ) {
			documentBuilder.objectField( "contained", b -> b
					.field( "value", containedValue ) );
		}
		builder.document( documentBuilder.build() );
	}
}
