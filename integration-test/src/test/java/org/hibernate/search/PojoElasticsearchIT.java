/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.model.spi.IndexModelCollector;
import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.backend.document.spi.IndexFieldReference;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackendFactory;
import org.hibernate.search.engine.bridge.builtin.impl.DefaultIntegerIdentifierBridge;
import org.hibernate.search.engine.bridge.declaration.spi.BridgeBeanReference;
import org.hibernate.search.engine.bridge.declaration.spi.BridgeMapping;
import org.hibernate.search.engine.bridge.mapping.BridgeDefinitionBase;
import org.hibernate.search.engine.bridge.spi.Bridge;
import org.hibernate.search.engine.bridge.spi.FunctionBridge;
import org.hibernate.search.engine.common.SearchManagerFactory;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.mapper.javabean.mapping.JavaBeanMapper;
import org.hibernate.search.mapper.javabean.mapping.JavaBeanMappingType;
import org.hibernate.search.engine.mapper.model.spi.Indexable;
import org.hibernate.search.engine.mapper.model.spi.IndexableModel;
import org.hibernate.search.engine.mapper.model.spi.IndexableReference;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MappingDefinition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class PojoElasticsearchIT {

	private SearchManagerFactory managerFactory;

	@Before
	public void setup() {
		MappingDefinition mapping = JavaBeanMapper.get().programmaticMapping();
		mapping.entity( IndexedEntity.class )
				.indexed( "IndexedEntity" )
				.bridge(
						new MyBridgeDefinition()
						.objectName( "customBridgeOnClass" )
				)
				.property( "id" )
						.documentId()
				.property( "text" )
						.field()
								.name( "myTextField" )
				.property( "embedded" )
						.indexedEmbedded()
								.maxDepth( 1 );

		MappingDefinition secondMapping = JavaBeanMapper.get().programmaticMapping();
		secondMapping.entity( ParentIndexedEntity.class )
				.property( "localDate" )
						.field()
								.name( "myLocalDateField" )
				.property( "embedded" )
						.bridge(
								new MyBridgeDefinition()
								.objectName( "customBridgeOnProperty" )
						);
		secondMapping.entity( OtherIndexedEntity.class )
				.indexed( "OtherIndexedEntity" )
				.property( "id" )
						.documentId().bridge( DefaultIntegerIdentifierBridge.class )
				.property( "numeric" )
						.field()
						.field().name( "numericAsString" ).bridge( IntegerAsStringFunctionBridge.class );

		managerFactory = SearchManagerFactory.builder()
				.addMapping( mapping )
				.addMapping( secondMapping )
				.setProperty( "backend.elasticsearchBackend_1.type", ElasticsearchBackendFactory.class.getName() )
				.setProperty( "backend.elasticsearchBackend_1.host", "http://es1.mycompany.com:9200/" )
				.setProperty( "backend.elasticsearchBackend_2.type", ElasticsearchBackendFactory.class.getName() )
				.setProperty( "backend.elasticsearchBackend_2.host", "http://es2.mycompany.com:9200/" )
				.setProperty( "index.default.backend", "elasticsearchBackend_1" )
				.setProperty( "index.OtherIndexedEntity.backend", "elasticsearchBackend_2" )
				.build();
	}

	@After
	public void cleanup() {
		if ( managerFactory != null ) {
			managerFactory.close();
		}
	}

	@Test
	public void index() {
		try ( PojoSearchManager manager = managerFactory.createSearchManager( JavaBeanMappingType.get() ) ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setText( "this is text (1)" );
			entity1.setLocalDate( LocalDate.of( 2017, 11, 1 ) );
			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setText( "some more text (2)" );
			entity2.setLocalDate( LocalDate.of( 2017, 11, 2 ) );
			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setText( "some more text (3)" );
			entity3.setLocalDate( LocalDate.of( 2017, 11, 3 ) );
			OtherIndexedEntity entity4 = new OtherIndexedEntity();
			entity4.setId( 4 );
			entity4.setNumeric( 404 );

			entity1.setEmbedded( entity2 );
			entity2.setEmbedded( entity3 );

			manager.getWorker().add( entity1 );
			manager.getWorker().add( entity2 );
			manager.getWorker().add( entity4 );
			manager.getWorker().delete( IndexedEntity.class, 1 );
			manager.getWorker().add( entity3 );
		}
	}

	public static class ParentIndexedEntity {

		private LocalDate localDate;

		private IndexedEntity embedded;

		public LocalDate getLocalDate() {
			return localDate;
		}

		public void setLocalDate(LocalDate localDate) {
			this.localDate = localDate;
		}

		public IndexedEntity getEmbedded() {
			return embedded;
		}

		public void setEmbedded(IndexedEntity embedded) {
			this.embedded = embedded;
		}

	}

	public static final class IndexedEntity extends ParentIndexedEntity {

		// TODO make it work with a primitive int too
		private Integer id;

		private String text;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

	}

	public static final class OtherIndexedEntity {

		// TODO make it work with a primitive int too
		private Integer id;

		private Integer numeric;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getNumeric() {
			return numeric;
		}

		public void setNumeric(Integer numeric) {
			this.numeric = numeric;
		}

	}

	public static final class IntegerAsStringFunctionBridge implements FunctionBridge<Integer, String> {
		@Override
		public String toDocument(Integer propertyValue) {
			return propertyValue == null ? null : propertyValue.toString();
		}
	}

	@BridgeMapping(implementation = @BridgeBeanReference(type = MyBridgeImpl.class))
	@Target(value = { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MyBridge {
		String objectName();
	}

	public static final class MyBridgeDefinition extends BridgeDefinitionBase<MyBridge> {

		@Override
		protected Class<MyBridge> getAnnotationClass() {
			return MyBridge.class;
		}

		public MyBridgeDefinition objectName(String value) {
			addParameter( "objectName", value );
			return this;
		}
	}

	public static final class MyBridgeImpl implements Bridge<MyBridge> {

		private MyBridge parameters;
		private IndexableReference<IndexedEntity> sourceRef;
		private IndexFieldReference<String> textFieldRef;
		private IndexFieldReference<LocalDate> localDateFieldRef;

		@Override
		public void initialize(BuildContext buildContext, MyBridge parameters) {
			this.parameters = parameters;
		}

		@Override
		public void bind(IndexableModel indexableModel, IndexModelCollector indexModelCollector) {
			sourceRef = indexableModel.asReference( IndexedEntity.class );
			IndexModelCollector objectRef = indexModelCollector.childObject( parameters.objectName() );
			textFieldRef = objectRef.field( "text" ).fromString().asReference();
			localDateFieldRef = objectRef.field( "date" ).fromLocalDate().asReference();
		}

		@Override
		public void toDocument(Indexable source, DocumentState target) {
			IndexedEntity sourceValue = source.get( sourceRef );
			if ( sourceValue != null ) {
				textFieldRef.add( target, sourceValue.getText() );
				localDateFieldRef.add( target, sourceValue.getLocalDate() );
			}
		}

	}

}
