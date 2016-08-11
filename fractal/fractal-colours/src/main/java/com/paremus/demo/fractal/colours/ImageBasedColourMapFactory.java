package com.paremus.demo.fractal.colours;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.paremus.demo.fractal.api.ColourMap;

/**
 * A factory which publishes a {@link ColourMap} for each of the images in the bundle
 */
@Component
public class ImageBasedColourMapFactory {

	private final List<ServiceRegistration<ColourMap>> registrations = new ArrayList<ServiceRegistration<ColourMap>>();
	
	@Activate
	void activate(BundleContext ctx) {
		Enumeration<URL> e = ctx.getBundle().findEntries("/colourSchemes", "*.png", false);
	
		while(e.hasMoreElements()) {
			URL nextElement = e.nextElement();
			String fileName = nextElement.getPath();
			fileName = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.length() - 4);
			try {
				ctx.registerService(ColourMap.class, new ImageBasedColourMap(nextElement), 
						new Hashtable<String, Object>(Collections.singletonMap(ColourMap.PROFILE_NAME, fileName)));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	@Deactivate
	void deactivate() {
		for(ServiceRegistration<?> reg : registrations) {
			reg.unregister();
		}
		registrations.clear();
	}

}