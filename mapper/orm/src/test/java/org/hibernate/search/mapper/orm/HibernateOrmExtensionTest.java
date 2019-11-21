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
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentFieldValueConvertContextImpl;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.orm.session.context.impl.HibernateOrmSessionContextImpl;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.IdentifierBridgeToDocumentIdentifierContextImpl;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.ValueBridgeToIndexedValueContextImpl;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

import org.junit.Test;

import org.easymock.EasyMockSupport;

public class HibernateOrmExtensionTest extends EasyMockSupport {
	private final SessionFactoryImplementor sessionFactoryImplementor = createMock( SessionFactoryImplementor.class );
	private final SessionImplementor sessionImplementor = createMock( SessionImplementor.class );
	private final HibernateOrmMappingContextImpl mappingContext =
			new HibernateOrmMappingContextImpl( sessionFactoryImplementor );
	private final PojoRuntimeIntrospector runtimeIntrospector = createMock( PojoRuntimeIntrospector.class );
	private final HibernateOrmSessionContextImpl sessionContext =
			new HibernateOrmSessionContextImpl( mappingContext, sessionImplementor, runtimeIntrospector );

	@Test
	public void identifierBridge() {
		IdentifierBridgeToDocumentIdentifierContext toDocumentContext =
				new IdentifierBridgeToDocumentIdentifierContextImpl( mappingContext );
		resetAll();
		replayAll();
		assertThat( toDocumentContext.extension( HibernateOrmExtension.get() ) ).isSameAs( mappingContext );
		verifyAll();

		IdentifierBridgeFromDocumentIdentifierContext fromDocumentContext = new BridgeSessionContext( sessionContext );
		resetAll();
		replayAll();
		assertThat( fromDocumentContext.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
		verifyAll();
	}

	@Test
	public void routingKeyBridge() {
		RoutingKeyBridgeToRoutingKeyContext context = new BridgeSessionContext( sessionContext );
		resetAll();
		replayAll();
		assertThat( context.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
		verifyAll();
	}

	@Test
	public void typeBridge() {
		TypeBridgeWriteContext context = new BridgeSessionContext( sessionContext );
		resetAll();
		replayAll();
		assertThat( context.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
		verifyAll();
	}

	@Test
	public void propertyBridge() {
		PropertyBridgeWriteContext context = new BridgeSessionContext( sessionContext );
		resetAll();
		replayAll();
		assertThat( context.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
		verifyAll();
	}

	@Test
	public void valueBridge() {
		ValueBridgeToIndexedValueContext toIndexedValueContext = new ValueBridgeToIndexedValueContextImpl( mappingContext );
		resetAll();
		replayAll();
		assertThat( toIndexedValueContext.extension( HibernateOrmExtension.get() ) ).isSameAs( mappingContext );
		verifyAll();

		ValueBridgeFromIndexedValueContext fromIndexedValueContext = new BridgeSessionContext( sessionContext );
		resetAll();
		replayAll();
		assertThat( fromIndexedValueContext.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
		verifyAll();
	}

	@Test
	public void toIndexValueConverter() {
		ToDocumentFieldValueConvertContext context = new ToDocumentFieldValueConvertContextImpl( mappingContext );
		resetAll();
		replayAll();
		assertThat( context.extension( HibernateOrmExtension.get() ) ).isSameAs( mappingContext );
		verifyAll();
	}

	@Test
	public void fromIndexValueConverter() {
		FromDocumentFieldValueConvertContext context = new FromDocumentFieldValueConvertContextImpl( sessionContext );
		resetAll();
		replayAll();
		assertThat( context.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
		verifyAll();
	}
}