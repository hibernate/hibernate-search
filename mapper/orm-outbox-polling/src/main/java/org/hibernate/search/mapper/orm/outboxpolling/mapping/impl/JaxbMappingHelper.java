/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.mapping.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class JaxbMappingHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private JaxbMappingHelper() {
	}

	public static String marshall(JaxbEntityMappings mappings) {
		try ( ByteArrayOutputStream out = new ByteArrayOutputStream() ) {
			JAXBContext context = JAXBContext.newInstance( JaxbEntityMappings.class );
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
			marshaller.marshal( mappings, out );
			return out.toString( StandardCharsets.UTF_8 );
		}
		catch (IOException | JAXBException e) {
			throw log.unableToProcessEntityMappings( e.getMessage(), e );
		}
	}

	public static JaxbEntityMappings unmarshall(String mappings) {
		try ( ByteArrayInputStream in = new ByteArrayInputStream( mappings.getBytes( StandardCharsets.UTF_8 ) ) ) {
			JAXBContext context = JAXBContext.newInstance( JaxbEntityMappings.class );
			Unmarshaller unmarshaller = context.createUnmarshaller();
			return (JaxbEntityMappings) unmarshaller.unmarshal( in );
		}
		catch (IOException | JAXBException e) {
			throw log.unableToProcessEntityMappings( e.getMessage(), e );
		}
	}
}
