package com.paremus.demo.fractal.http.jaxrs;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This REST service provides the basic JSON data for populating the form controls
 */
@Path("/config")
public class FormDataProvider {

	private Collection<Map<String, Object>> equationProperties;
	private Set<String> colourSchemes;

	public FormDataProvider(Collection<Map<String, Object>> equationProperties,
			Set<String> colourSchemes) {
				this.equationProperties = equationProperties;
				this.colourSchemes = colourSchemes;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response doGet() throws IOException {
		
		StringWriter writer = new StringWriter();
		JsonGenerator generator = new JsonFactory(new ObjectMapper()).createJsonGenerator(writer);
		
		generator.writeStartObject();
		
		//Write the equation objects
		generator.writeFieldName("equations");
		generator.writeObject(equationProperties);
		
		//Write the colour scheme names
		generator.writeFieldName("colourSchemes");
		generator.writeObject(colourSchemes);
		
		generator.writeEndObject();
		generator.close();
		
		
		Response response = Response.status(Status.OK)
				.cacheControl(CacheControl.valueOf("no-cache"))
				.entity(writer.toString()).build();
		return response;
	}
	
}

