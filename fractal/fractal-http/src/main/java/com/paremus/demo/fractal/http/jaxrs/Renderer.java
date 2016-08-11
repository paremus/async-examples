package com.paremus.demo.fractal.http.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.osgi.service.async.Async;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.osgi.util.promise.Success;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paremus.demo.fractal.api.ColourMap;
import com.paremus.demo.fractal.api.Equation;

/**
 * This REST service produces a series of JSON events representing parts of the image
 * that should be rendered on the client.
 */
@Path("/render")
public class Renderer {
	
	/**
	 * An abstract handler for doing the rendering. It splits the overall
	 * image into chunks and renders them individually. How the rendering
	 * service is called is left up to the subclasses.
	 */
	private abstract class AbstractRenderOutput implements StreamingOutput {
		
		protected final double minX;
		protected final double maxX;
		protected final double minY;
		protected final double maxY;

		protected final int maxIterations;
		protected final ColourMap colourMap;
		protected final Equation eqn;
		
		private AbstractRenderOutput(double minX, double maxX, double minY, double maxY, 
				int maxIterations, ColourMap colourMap, Equation eqn) {
			
			this.minX = minX;
			this.maxX = maxX;
			this.minY = minY;
			this.maxY = maxY;

			this.maxIterations = maxIterations;
			this.colourMap = colourMap;
			this.eqn = eqn;
		}
		
		@Override
		public void write(OutputStream out) throws IOException,
		WebApplicationException {
			try {
				int blockWidth = CANVAS_WIDTH / 8;
				int blockHeight = CANVAS_HEIGHT / 8;
				
				double deltaX = (maxX - minX) / CANVAS_WIDTH;
				double deltaY = (maxY - minY) / CANVAS_HEIGHT;
				
				for(int y = 0; y < 8; y++) {
					for(int x = 0; x < 8; x++) {
						
						int xOffset = x * blockWidth;
						int yOffset = y * blockHeight;
						
						render(out, blockWidth, blockHeight, deltaX, deltaY, xOffset, yOffset);
					}
				}
				
				awaitCompletion();
			} finally {
				synchronized (out) {
					out.write("data: { \"terminate\" : true }\n\n".getBytes("UTF-8"));
				}
			}
		}
		
		/**
		 * Called to render a piece of the image
		 * 
		 * @param out
		 * @param width
		 * @param height
		 * @param deltaX
		 * @param deltaY
		 * @param xOffset
		 * @param yOffset
		 * @throws IOException
		 */
		protected abstract void render(OutputStream out, int width, int height, double deltaX, double deltaY, 
				int xOffset, int yOffset) throws IOException;
		
		/**
		 * Called once all pieces have received rendering requests. Should block until the final event can be sent.
		 */
		protected abstract void awaitCompletion();
	}
	
	/**
	 * An {@link AbstractRenderOutput} that synchronously calls the Equation and writes out the data
	 */
	private class SynchronousRender extends AbstractRenderOutput {

		public SynchronousRender(double minX, double maxX,
				double minY, double maxY, int maxIterations, ColourMap colourMap,
				Equation eqn) {
			super(minX, maxX, minY, maxY, maxIterations, colourMap, eqn);
		}

		@Override
		protected void render(OutputStream out, int width, int height,
				double deltaX, double deltaY, int xOffset, int yOffset) throws IOException {
			
			int[][] values = eqn.execute(width, height, minX + xOffset * deltaX, deltaX, 
					maxY - yOffset * deltaY, deltaY, maxIterations, colourMap.getSpectrum().length);
	
			writeOutChunk(out, maxIterations, xOffset, yOffset, values, colourMap);
		}

		@Override
		protected void awaitCompletion() {
			// A no-op
		}
	}

	/**
	 * An {@link AbstractRenderOutput} that asynchronously calls the Equation and writes
	 * out the data when it is eventually available.
	 */
	private class AsynchronousRender extends AbstractRenderOutput {
		
		private final List<Promise<Void>> pendingWork = new ArrayList<Promise<Void>>();
		
		public AsynchronousRender(double minX, double maxX, double minY, double maxY, 
				int maxIterations, ColourMap colourMap, Equation eqn) {
			super(minX, maxX, minY, maxY, maxIterations, colourMap, async.mediate(eqn, Equation.class));
		}
		
		@Override
		protected void render(final OutputStream out, int width, int height,
				double deltaX, double deltaY, final int xOffset, final int yOffset) throws IOException {
			
			//This promise will resolve when calculation is complete
			
			Promise<int[][]> values = async.call(eqn.execute(width, height, 
					minX + xOffset * deltaX, deltaX, maxY - yOffset * deltaY, deltaY, 
					maxIterations, colourMap.getSpectrum().length));
			
			// Store the chained promise, not the original. We want to wait until the data
			// has been completely written.
			pendingWork.add(values.then(new Success<int[][], Void>() {
				@Override
				public Promise<Void> call(Promise<int[][]> complete)
						throws Exception {
					writeOutChunk(out, maxIterations, xOffset, yOffset, complete.getValue(), colourMap);
					return null;
				}
			}));
		}
		
		@Override
		protected void awaitCompletion() {
			// Use the latch function in the Promises class to wait until all the data is written.
			try {
				Promises.all(pendingWork).getValue();
			} catch (InvocationTargetException e) {
				throw new WebApplicationException(e.getTargetException());
			} catch (InterruptedException e) {
				throw new WebApplicationException(e);
			}
		}
	}


	private static final int CANVAS_WIDTH = 480;
	private static final int CANVAS_HEIGHT = 416;
	
	private final ConcurrentMap<String, Equation> equations;

	private final ConcurrentMap<String, ColourMap> colourSchemes;
	
	private final Async async;
	
	public Renderer(ConcurrentMap<String, Equation> equations,
			ConcurrentMap<String, ColourMap> colourSchemes,
			Async async) {
		this.equations = equations;
		this.colourSchemes = colourSchemes;
		this.async = async;
	}

	
	@GET
	@Path("/sync/{minX}/{maxX}/{minY}/{maxY}/{maxIterations}/{equation}/{colourScheme}")
	@Produces("text/event-stream")
	public Response renderFractal(@PathParam("minX") final double minX, @PathParam("maxX") final double maxX, 
			@PathParam("minY") final double minY, @PathParam("maxY") final double maxY, 
			@PathParam("maxIterations") final int maxIterations, @PathParam("colourScheme") String colourScheme, 
			@PathParam("equation") String equation) {
		
		final Equation eqn = equations.get(equation);
		final ColourMap colourMap = colourSchemes.get(colourScheme);
		try {
			validateParameters(minX, maxX, minY, maxY, maxIterations, 
					eqn, equation, colourMap, colourScheme);
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST)
					.entity(e.getMessage())
					.build();
		}
		
		return Response.ok(new SynchronousRender(minX, maxX, minY, maxY, maxIterations,
				colourMap, eqn)).build();
	}

	@GET
	@Path("/async/{minX}/{maxX}/{minY}/{maxY}/{maxIterations}/{equation}/{colourScheme}")
	@Produces("text/event-stream")
	public Response asyncRenderFractal(@PathParam("minX") final double minX, @PathParam("maxX") final double maxX, 
			@PathParam("minY") final double minY, @PathParam("maxY") final double maxY, 
			@PathParam("maxIterations") final int maxIterations, @PathParam("colourScheme") String colourScheme, 
			@PathParam("equation") String equation) {
		
		final Equation eqn = equations.get(equation);
		final ColourMap colourMap = colourSchemes.get(colourScheme);
		try {
			validateParameters(minX, maxX, minY, maxY, maxIterations, 
					eqn, equation, colourMap, colourScheme);
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST)
					.entity(e.getMessage())
					.build();
		}
		
		return Response.ok(new AsynchronousRender(minX, maxX, minY, maxY, maxIterations, 
				colourMap, eqn)).build();
	}


	private void validateParameters(double minX, double maxX, double minY, double maxY, int maxIterations, 
			Equation eqn, String equation, ColourMap colourMap, String colourScheme) {
		
		if(minX >= maxX) 
			throw new IllegalArgumentException("The minimum x coordinate must be less than the maximum");
		if(minY >= maxY) 
			throw new IllegalArgumentException("The minimum y coordinate must be less than the maximum");
		
		if(maxIterations < 1) 
			throw new IllegalArgumentException("There must be a positive number of iterations");
		
		if(eqn == null) 
			throw new IllegalArgumentException("No matching equation for " + equation);
		
		if(colourMap == null) 
			throw new IllegalArgumentException("No matching colour scheme for " + colourScheme);
	}


	/**
	 * Write out an event containing a block of rendered pixels
	 * @param out
	 * @param maxIterations
	 * @param xOffset
	 * @param yOffset
	 * @param values
	 * @param colourMap
	 * @throws IOException
	 */
	private void writeOutChunk(OutputStream out, int maxIterations,
			int xOffset, int yOffset, int[][] values, ColourMap colourMap) throws IOException {
		
		synchronized(out) {
			out.write("data: ".getBytes("UTF-8"));
			
			JsonGenerator generator = new JsonFactory(new ObjectMapper()).createJsonGenerator(out);
			
			generator.writeStartObject();
			
			generator.writeNumberField("x", xOffset);
			generator.writeNumberField("y", yOffset);
			generator.writeFieldName("data");
			generator.writeObject(values);
			generator.writeFieldName("colours");
			generator.writeObject(colourMap.getSpectrum());
			
			generator.writeEndObject();
			
			generator.flush();
			
			out.write("\n\n".getBytes("UTF-8"));
			out.flush();
		}
	}
}
