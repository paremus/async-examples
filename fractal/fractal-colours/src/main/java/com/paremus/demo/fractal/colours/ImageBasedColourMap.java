package com.paremus.demo.fractal.colours;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.Arrays;

import com.paremus.demo.fractal.api.ColourMap;

/**
 * A {@link ColourMap} that uses the top row of pixels from an image to build its values
 */
public class ImageBasedColourMap implements ColourMap {

	private final String[] colours;
	
	public ImageBasedColourMap(URL in) throws IOException {
        Image image = Toolkit.getDefaultToolkit().createImage(in);
        
        int w = readWidth(image);

        int[] pixels = new int[w];

        PixelGrabber pg = new PixelGrabber(image, 0, 0, w, 1, pixels, 0, w);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            throw new InterruptedIOException("Image load interrupted");
        }

        if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
            throw new IOException("Image fetch aborted or errored");
        }

        colours = new String[w];
        
        //Convert each RGB pixel into a #AABBCC value, remembering to include leading zeros.
        for(int idx = 0; idx < w; idx++) {
        	int pixel = pixels[idx];
        	StringBuilder sb = new StringBuilder("#");
        	colours[idx] = sb
        		.append(Integer.toHexString((0xF00000 & pixel) >>> 20))
        		.append(Integer.toHexString((0xF0000 & pixel) >>> 16))
        		.append(Integer.toHexString((0xF000 & pixel) >>> 12))
        		.append(Integer.toHexString((0xF00 & pixel) >>> 8))
        		.append(Integer.toHexString((0xF0 & pixel) >>> 4))
        		.append(Integer.toHexString((0xF & pixel)))
        		.toString();
        }
        
    }

	
	private int readWidth(Image image) throws IOException {
        final int[] dump = new int[1];
        dump[0] = -1;

        ImageObserver observer = new ImageObserver() {

            public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
                if ((infoflags & ImageObserver.WIDTH) != 0) {
                    synchronized (dump) {
                        dump[0] = width;
                        dump.notify();
                    }
                }

                return (infoflags & ImageObserver.ALLBITS) != 0;
            }

        };

        int w = image.getWidth(observer);

        if (w == -1) {
            synchronized (dump) {
                while (dump[0] == -1) {
                    try {
                        dump.wait();
                    } catch (InterruptedException ie) {
                        throw new InterruptedIOException("Image load interrupted");
                    }
                }
            }

            w = dump[0];
        }

        return w;
    }


	@Override
	public String[] getSpectrum() {
		return Arrays.copyOf(colours, colours.length);
	}
}
