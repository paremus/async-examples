package com.paremus.demo.fractal.http;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * A simple handler that delivers static content, and redirects if the root path doesn't contain a trailing slash
 */
@Component(service = ServletContextHelper.class,
	property = {
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=redirect",
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH + "=" + RedirectingStaticContentHandler.ROOT_APP_PATH,
			HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN + "=/*",
			HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX + "=/static",
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=redirect)"
	}
)
public class RedirectingStaticContentHandler extends ServletContextHelper {
	
	static final String ROOT_APP_PATH = "/paremus/demo/fractal";
	
	/**
	 * Redirect any request that comes in for the root with no trailing /.
	 * If we don't do this then the css and javascript can't be found properly.
	 */
	@Override
	public boolean handleSecurity(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		if(ROOT_APP_PATH.equals(request.getRequestURI())) {
			response.sendRedirect(ROOT_APP_PATH + "/");
			return false;
		}
		return true;
	}

	/**
	 * Default to serving the main page if the user requests the root application path.
	 * Otherwise give them what they ask for.
	 */
	@Override
	public URL getResource(String name) {
		if("/static/".equals(name)) {
			name = "/static/index.html";
		}
		return FrameworkUtil.getBundle(RedirectingStaticContentHandler.class).getEntry(name);
	}

	/**
	 * Use default mime types.
	 */
	@Override
	public String getMimeType(String name) {
		return null;
	}
}