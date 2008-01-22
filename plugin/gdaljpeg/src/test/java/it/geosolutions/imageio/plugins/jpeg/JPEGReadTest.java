/*
 *    JImageIO-extension - OpenSource Java Image translation Library
 *    http://www.geo-solutions.it/
 *	  https://imageio-ext.dev.java.net/
 *    (C) 2007, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.imageio.plugins.jpeg;

import it.geosolutions.imageio.gdalframework.Viewer;
import it.geosolutions.resources.TestData;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import com.sun.imageio.plugins.jpeg.JPEGImageReaderSpi;
import com.sun.media.jai.codecimpl.util.RasterFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Testing reading capabilities for {@link JpegGDALImageReader} leveraging on
 * JAI.
 * 
 * @author Daniele Romagnoli, GeoSolutions.
 * @author Simone Giannecchini, GeoSolutions.
 */
public class JPEGReadTest extends AbstractJPEGTestCase {
	public JPEGReadTest(String name) {
		super(name);
	}

	/**
	 * Simple test read
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void testRead() throws FileNotFoundException, IOException {
		final ParameterBlockJAI pbjImageRead;
		final ImageReadParam irp = new ImageReadParam();
		final String fileName = "sample.jpg";
		final File file = TestData.file(this, fileName);
		irp.setSourceSubsampling(4, 4, 0, 0);
		pbjImageRead = new ParameterBlockJAI("ImageRead");
		pbjImageRead.setParameter("Input", file);
		pbjImageRead.setParameter("Reader", new JpegGDALImageReaderSpi()
				.createReaderInstance());
		pbjImageRead.setParameter("readParam", irp);

		final ImageLayout l = new ImageLayout();
		l.setTileGridXOffset(0).setTileGridYOffset(0).setTileHeight(512)
				.setTileWidth(512);

		RenderedOp image = JAI.create("ImageRead", pbjImageRead,
				new RenderingHints(JAI.KEY_IMAGE_LAYOUT, l));
		if (TestData.isInteractiveTest())
			Viewer.visualize(image);
		else
			assertNotNull(image.getTiles());
	}

	/**
	 * Test sourceBands management capabilities.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void testSourceBands() throws IOException, FileNotFoundException {
		final File inputFile = TestData.file(this, "mountain.jpg");

		// //
		//
		// Preparing srcRegion constants
		//
		// //
		final int srcRegionX = 0;
		final int srcRegionY = 0;
		final int srcRegionWidth = 400;
		final int srcRegionHeight = 200;
		final int subSamplingX = 2;
		final int subSamplingY = 1;

		// //
		//
		// Setting source settings parameters
		//
		// //
		ImageReadParam rparam = new ImageReadParam();
		rparam.setSourceRegion(new Rectangle(srcRegionX, srcRegionY,
				srcRegionWidth, srcRegionHeight));
		rparam.setSourceSubsampling(subSamplingX, subSamplingY, 0, 0);
		rparam.setSourceBands(new int[] { 0 });

		// //
		//
		// Setting destination settings parameters
		//
		// //
		rparam.setDestinationBands(new int[] { 0 });

		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		ColorModel cm = RasterFactory.createComponentColorModel(
				DataBuffer.TYPE_BYTE, // dataType
				cs, // color space
				false, // has alpha
				false, // is alphaPremultiplied
				Transparency.OPAQUE); // transparency

		final int destWidth = srcRegionWidth / subSamplingX;
		final int destHeight = srcRegionHeight / subSamplingY;
		assertEquals(destWidth, 200);
		assertEquals(destHeight, 200);

		final SampleModel sm = cm.createCompatibleSampleModel(destWidth,
				destHeight);
		rparam.setDestination(new BufferedImage(cm,
				javax.media.jai.RasterFactory.createWritableRaster(sm,
						new Point(0, 0)), false, null));

		// //
		//
		// Preparing for image read operation
		//
		// //
		ImageReader reader = new JpegGDALImageReaderSpi()
				.createReaderInstance();
		final ParameterBlockJAI pbjImageRead = new ParameterBlockJAI(
				"ImageRead");
		pbjImageRead.setParameter("Input", inputFile);
		pbjImageRead.setParameter("reader", reader);
		pbjImageRead.setParameter("readParam", rparam);
		final ImageLayout l = new ImageLayout();
		l.setTileGridXOffset(0).setTileGridYOffset(0).setTileHeight(256)
				.setTileWidth(256).setSampleModel(sm);

		RenderedOp image = JAI.create("ImageRead", pbjImageRead,
				new RenderingHints(JAI.KEY_IMAGE_LAYOUT, l));

		if (TestData.isInteractiveTest())
			Viewer.visualize(image);
		else
			assertNotNull(image.getTiles());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();

//		// Test reading of a simple image
//		suite.addTest(new JPEGReadTest("testRead"));

		// Test reading of a simple image
		suite.addTest(new JPEGReadTest("testSourceBands"));

		return suite;
	}

	public static void main(java.lang.String[] args) {
		junit.textui.TestRunner.run(suite());
	}

}
