/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.mapping.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.ConfigurationLog;

public class JaxbMappingHelper {

	private JaxbMappingHelper() {
	}

	public static String marshall(JaxbEntityMappingsImpl mappings) {
		try ( ByteArrayOutputStream out = new ByteArrayOutputStream() ) {
			JAXBContext context = JAXBContext.newInstance( JaxbEntityMappingsImpl.class );
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
			marshaller.marshal( mappings, out );
			return out.toString( StandardCharsets.UTF_8 );
		}
		catch (IOException | JAXBException e) {
			throw ConfigurationLog.INSTANCE.unableToProcessEntityMappings( e.getMessage(), e );
		}
	}

	public static JaxbEntityMappingsImpl unmarshall(String mappings) {
		try ( ByteArrayInputStream in = new ByteArrayInputStream( mappings.getBytes( StandardCharsets.UTF_8 ) ) ) {
			JAXBContext context = JAXBContext.newInstance( JaxbEntityMappingsImpl.class );
			Unmarshaller unmarshaller = context.createUnmarshaller();
			return (JaxbEntityMappingsImpl) unmarshaller.unmarshal( in );
		}
		catch (IOException | JAXBException e) {
			throw ConfigurationLog.INSTANCE.unableToProcessEntityMappings( e.getMessage(), e );
		}
	}
}
