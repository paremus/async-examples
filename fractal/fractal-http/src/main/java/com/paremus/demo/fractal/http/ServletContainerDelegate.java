package com.paremus.demo.fractal.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.service.async.Async;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import com.paremus.demo.fractal.api.ColourMap;
import com.paremus.demo.fractal.api.Equation;
import com.paremus.demo.fractal.http.jaxrs.FormDataProvider;
import com.paremus.demo.fractal.http.jaxrs.Renderer;
import com.paremus.http.publish.HttpEndpointPublisher;

@Component(service = Servlet.class,
	property = {
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/rest/*",
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=redirect)",
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME + "=Fractal Viewer",
			HttpEndpointPublisher.APP_ENDPOINT + "=/"
	}
)
public class ServletContainerDelegate implements Servlet {
	
	/** The async service to use when making asynchronous calls */
	@Reference
	Async asyncService;
	
	/** The Equation Services */
	private final ConcurrentMap<String, Equation> equations = 
			new ConcurrentHashMap<String, Equation>();
	
	/** Available Equations, and their default configurations. Sort by name for UI consistency */
	private final ConcurrentMap<String, Map<String, Object>> equationConfig = 
			new ConcurrentSkipListMap<String, Map<String, Object>>();

	/** Available Colour Schemes. Sort by name for UI consistency */
	private final ConcurrentMap<String, ColourMap> colourSchemes = 
			new ConcurrentSkipListMap<String, ColourMap>();

	/** The Jersey REST container */
	private volatile ServletContainer container;

	@Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
	void addEquation(Equation equation, Map<String, Object> props) {
		equations.putIfAbsent(String.valueOf(props.get(Equation.EQUATION_TYPE)), equation);
		equationConfig.putIfAbsent(String.valueOf(props.get(Equation.EQUATION_TYPE)), new HashMap<String, Object>(props));
	}

	void removeEquation(Equation equation, Map<String, Object> props) {
		equationConfig.remove(String.valueOf(props.get(Equation.EQUATION_TYPE)), equation);
	}

	@Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, service=ColourMap.class)
	void addColourMap(ColourMap colourMap, Map<String, Object> props) {
		colourSchemes.putIfAbsent(String.valueOf(props.get(ColourMap.PROFILE_NAME)), colourMap);
	}

	void removeColourMap(ColourMap colourMap, Map<String, Object> props) {
		colourSchemes.remove(String.valueOf(props.get(ColourMap.PROFILE_NAME)), colourMap);
	}

	@Activate
	void start() {
		// Set up the REST services
		ResourceConfig config = new ResourceConfig();
		config.registerInstances(
				new FormDataProvider(equationConfig.values(), colourSchemes.keySet()),
				new Renderer(equations, colourSchemes, asyncService)
			);
		
		this.container = new ServletContainer(config);
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		container.init(config);
	}

	@Override
	public ServletConfig getServletConfig() {
		return container.getServletConfig();
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		container.service(req, res);
	}

	@Override
	public String getServletInfo() {
		return container.getServletInfo();
	}

	@Deactivate
	@Override
	public void destroy() {
		container.destroy();
	}
	
}
