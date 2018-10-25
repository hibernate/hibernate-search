/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.spi.FromIndexFieldValueConvertContextImpl;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.orm.session.context.impl.HibernateOrmSessionContextImpl;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.BridgeSessionContextImpl;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.IdentifierBridgeToDocumentIdentifierContextImpl;

import org.junit.Test;

import org.easymock.EasyMockSupport;

public class OrmExtensionTest extends EasyMockSupport {
	private final SessionFactoryImplementor sessionFactoryImplementor = createMock( SessionFactoryImplementor.class );
	private final SessionImplementor sessionImplementor = createMock( SessionImplementor.class );
	private final HibernateOrmMappingContextImpl mappingContext =
			new HibernateOrmMappingContextImpl( sessionFactoryImplementor );
	private final HibernateOrmSessionContextImpl sessionContext =
			new HibernateOrmSessionContextImpl( mappingContext, sessionImplementor );

	@Test
	public void identifierBridge() {
		IdentifierBridgeToDocumentIdentifierContext toDocumentContext =
				new IdentifierBridgeToDocumentIdentifierContextImpl( mappingContext );
		resetAll();
		replayAll();
		assertThat( toDocumentContext.extension( OrmExtension.get() ) ).isSameAs( mappingContext );
		verifyAll();

		IdentifierBridgeFromDocumentIdentifierContext fromDocumentContext = new BridgeSessionContextImpl( sessionContext );
		resetAll();
		replayAll();
		assertThat( fromDocumentContext.extension( OrmExtension.get() ) ).isSameAs( sessionContext );
		verifyAll();
	}

	@Test
	public void routingKeyBridge() {
		RoutingKeyBridgeToRoutingKeyContext context = new BridgeSessionContextImpl( sessionContext );
		resetAll();
		replayAll();
		assertThat( context.extension( OrmExtension.get() ) ).isSameAs( sessionContext );
		verifyAll();
	}

	@Test
	public void typeBridge() {
		TypeBridgeWriteContext context = new BridgeSessionContextImpl( sessionContext );
		resetAll();
		replayAll();
		assertThat( context.extension( OrmExtension.get() ) ).isSameAs( sessionContext );
		verifyAll();
	}

	@Test
	public void propertyBridge() {
		PropertyBridgeWriteContext context = new BridgeSessionContextImpl( sessionContext );
		resetAll();
		replayAll();
		assertThat( context.extension( OrmExtension.get() ) ).isSameAs( sessionContext );
		verifyAll();
	}

	@Test
	public void fromIndexValueConverter() {
		FromIndexFieldValueConvertContext context = new FromIndexFieldValueConvertContextImpl( sessionContext );
		resetAll();
		replayAll();
		assertThat( context.extension( OrmExtension.get() ) ).isSameAs( sessionContext );
		verifyAll();
	}
}