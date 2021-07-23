/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.service.ServiceRegistry;

import org.jboss.jandex.IndexView;

@SuppressWarnings("deprecation")
public class OutboxAdditionalJaxbMappingProducer implements org.hibernate.boot.spi.AdditionalJaxbMappingProducer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Must not be longer than 20 characters, so that the generator does not exceed the 30 characters for Oracle11g
	private static final String OUTBOX_TABLE_NAME = "HSEARCH_OUTBOX_TABLE";

	private static final String DEFAULT_CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";
	private static final String MSSQL_CURRENT_TIMESTAMP = "SYSDATETIME()";

	private static final String DEFAULT_TYPE_TIMESTAMP = "TIMESTAMP";
	private static final String MYSQL_TYPE_TIMESTAMP = "TIMESTAMP(6)";
	private static final String MSSQL_TYPE_TIMESTAMP = "datetime2";

	private static final String TYPE_TIMESTAMP_PLACEHOLDER = "{{type_timestamp}}";
	private static final String CURRENT_TIMESTAMP_PLACEHOLDER = "{{current_timestamp}}";

	private static final String OUTBOX_ENTITY_DEFINITION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
	"\n" +
	"<hibernate-mapping>\n" +
	"    <class name=\"" + OutboxEvent.class.getName() + "\" table=\"" + OUTBOX_TABLE_NAME + "\">\n" +
	"        <id name=\"id\" column=\"ID\" type=\"long\">\n" +
	"            <generator class=\"org.hibernate.id.enhanced.SequenceStyleGenerator\">\n" +
	"                <param name=\"sequence_name\">" + OUTBOX_TABLE_NAME + "_GENERATOR</param>\n" +
	"                <param name=\"table_name\">" + OUTBOX_TABLE_NAME + "_GENERATOR</param>\n" +
	"                <param name=\"initial_value\">1</param>\n" +
	"                <param name=\"increment_size\">1</param>\n" +
	"            </generator>\n" +
	"        </id>\n" +
	" 		 <property name=\"moment\" generated=\"insert\" index=\"moment\">\n" +
	"     		 <column sql-type=\"" + TYPE_TIMESTAMP_PLACEHOLDER + "\" updatable=\"false\" default=\"" + CURRENT_TIMESTAMP_PLACEHOLDER + "\" />\n" +
	" 		 </property>\n" +
	"		 <property name=\"type\" >\n" +
	" 			 <type name=\"org.hibernate.type.EnumType\">\n" +
	" 				 <param name=\"enumClass\">" + OutboxEvent.Type.class.getName() + "</param>\n" +
	" 			 </type>\n" +
	"		 </property>\n" +
	"        <property name=\"entityName\" type=\"string\" />\n" +
	"        <property name=\"entityId\" type=\"string\" />\n" +
	"        <property name=\"payload\" type=\"binary\" length=\"8192\" />\n" +
	"        <property name=\"retries\" type=\"integer\" />\n" +
	"    </class>\n" +
	"</hibernate-mapping>\n";

	@Override
	public Collection<MappingDocument> produceAdditionalMappings(final MetadataImplementor metadata,
			IndexView jandexIndex, final MappingBinder mappingBinder, final MetadataBuildingContext buildingContext) {
		ServiceRegistry serviceRegistry = metadata.getMetadataBuildingOptions().getServiceRegistry();
		ConfigurationService service = serviceRegistry.getService( ConfigurationService.class );
		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

		Object customIndexingStrategy = service.getSettings().get( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_STRATEGY );
		if ( !AutomaticIndexingStrategyNames.OUTBOX_POLLING.equals( customIndexingStrategy ) ) {
			return Collections.emptyList();
		}

		Dialect dialect = jdbcServices.getJdbcEnvironment().getDialect();
		SQLFunction timestampFunction = dialect.getFunctions().get( "current_timestamp" );

		String typeTimestamp = ( dialect instanceof MySQLDialect ) ? MYSQL_TYPE_TIMESTAMP :
				( dialect instanceof SQLServerDialect ) ? MSSQL_TYPE_TIMESTAMP : DEFAULT_TYPE_TIMESTAMP;
		String currentTimestamp = ( dialect instanceof SQLServerDialect ) ? MSSQL_CURRENT_TIMESTAMP :
				( timestampFunction == null ) ? DEFAULT_CURRENT_TIMESTAMP :
				timestampFunction.render( null, Collections.emptyList(), null );

		String outboxSchema = OUTBOX_ENTITY_DEFINITION
				.replace( TYPE_TIMESTAMP_PLACEHOLDER, typeTimestamp )
				.replace( CURRENT_TIMESTAMP_PLACEHOLDER, currentTimestamp );

		log.outboxGeneratedEntityMapping( outboxSchema );
		Origin origin = new Origin( SourceType.OTHER, "search" );

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( outboxSchema.getBytes() );
		BufferedInputStream bufferedInputStream = new BufferedInputStream( byteArrayInputStream );
		Binding binding = mappingBinder.bind( bufferedInputStream, origin );

		JaxbHbmHibernateMapping root = (JaxbHbmHibernateMapping) binding.getRoot();

		MappingDocument mappingDocument = new MappingDocument( root, origin, buildingContext );
		return Collections.singletonList( mappingDocument );
	}
}
