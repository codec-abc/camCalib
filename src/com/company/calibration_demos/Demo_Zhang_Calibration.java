package com.company.calibration_demos;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.System.*;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import imagingbook.calibration.zhang.Calibrator;
import imagingbook.calibration.zhang.Calibrator.Parameters;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.calibration.zhang.testdata.ZhangData;
import imagingbook.calibration.zhang.util.GridPainter;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.settings.PrintPrecision;
import imagingbook.lib.util.ResourceUtils;

import static java.lang.System.out;

/**
 * This plugin performs Zhang's camera calibration on the
 * pre-calculated point data for the N given target views.
 * Based on the estimated intrinsic and extrinsic (view)
 * parameters, the corner points of the 3D target model are
 * then projected onto the corresponding calibration images
 * (a stack).
 * All rendering is done by pixel drawing (no graphic overlays).
 * 
 * @author W. Burger
 * @version 2017-05-30
 */
public class Demo_Zhang_Calibration implements PlugIn {
	
	static Class<?> resourceRootClass = ZhangData.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";
	
	static boolean ShowObservedModelPoints = true;		// draw observed image points into a new stack
	static boolean ShowProjectedImagePoints = true;		// draw projected image points into the test image stack
	static boolean ListCameraViews = true;
	static boolean BeVerbose = false;
	
	static Color BackGroundColor = Color.white;
	
	static {
		//jLogStream.redirectSystem();
		//PrintPrecision.set(6);
	}

	static String readFile(String path, Charset encoding)
			throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	static int countLines(String str){
		String[] lines = str.split("\r\n|\r|\n");
		return  lines.length;
	}

	public static void main (String[] args)
	{
		out.println("starting app");

		Demo_Zhang_Calibration calibApp = new Demo_Zhang_Calibration();
		calibApp.run(null);
	}
	
	public void run(String arg0) {
		/*
		ImagePlus testIm = ResourceUtils.openImageFromResource(resourceRootClass, resourceDir, resourceName);
		if (testIm == null) {
			IJ.error("Could not open calibration images!"); 
			return;
		}
		
		int M = testIm.getNSlices(); 	// number of views
		if (M < 2) {
			IJ.error("Image must be a stack with 2+ images!"); 
			return;
		}
		
		testIm.show();
		int width = testIm.getWidth();
		int height = testIm.getHeight();
		Point2D[] modelPoints = ZhangData.getModelPoints();
		Camera camReal = ZhangData.getCameraIntrinsics();
		if (BeVerbose)
			camReal.print("camReal");
		
//		ViewTransform[] viewsReal = ZhangData.getAllViewTransforms();	
		Point2D[][] obsPoints 	  = ZhangData.getAllObservedPoints();
			
		if (ShowObservedModelPoints){
			ImageStack stack = new ImageStack(width, height);
			drawSquares(stack, obsPoints);
			new ImagePlus("Observed points", stack).show();
		}
*/
		// perform calibration ------------------------------------------

		Point2D[] modelPoints = null;
		Point2D[][] obsPoints = null;
		int M = 5;

		try {
			String fileAsStr = readFile("data/mydata/model.txt", StandardCharsets.UTF_8);
			int nbLine = countLines(fileAsStr);

			modelPoints = new Point2D[nbLine];
			obsPoints = new Point2D[5][nbLine];
			String[] lines = fileAsStr.split("\r\n|\r|\n");
			int index = 0;
			for (String line: lines) {
				String[] numbers = line.split(" ");
				double x = Double.parseDouble(numbers[0]);
				double y = Double.parseDouble(numbers[1]);
				modelPoints[index] = new Point2D.Double(x, y);
				index++;
			}
		}
		catch (IOException e)
		{
			out.println("Exception " + e.getMessage());
		}

		for (int i = 1; i < 5 + 1; i++) {
			try {
				String fileAsStr = readFile("data/mydata/data" + i + ".txt", StandardCharsets.UTF_8);
				String[] lines = fileAsStr.split("\r\n|\r|\n");
				int index = 0;
				for (String line : lines) {
					String[] numbers = line.split(" ");
					double x = Double.parseDouble(numbers[0]);
					double y = Double.parseDouble(numbers[1]);
					obsPoints[i - 1][index] = new Point2D.Double(x, y);
					index++;
				}
			} catch (IOException e) {
				out.println("Exception " + e.getMessage());
			}
		}

		Parameters params = new Calibrator.Parameters();
		params.normalize = true;
		params.assumeZeroSkew = false;
		params.lensDistortionKoeffients = 2;
		params.beVerbose = BeVerbose;
		
		Calibrator zcalib = new Calibrator(params, modelPoints);
		for (int i = 0; i < M; i++) {
			zcalib.addView(obsPoints[i]);
		}
		
		Camera camFinal = zcalib.calibrate();
		if (camFinal == null) {
			out.println("calibration failed");
			return;
		}
		
		// show results ------------------------------------------
		
		camFinal.print("camFinal");

		double alpha = camFinal.getAlpha();
		double beta = camFinal.getBeta();
		double gamma = camFinal.getGamma();
		double uc = camFinal.getUc();
		double vc = camFinal.getVc();

		ViewTransform[] finalViews = zcalib.getFinalViews();
		
		if (ListCameraViews) {
			for (int i = 0; i < M; i++) {
				out.println("View " + i);
				finalViews[i].print();
			}
		}

		/*
		if (ShowProjectedImagePoints) {
			Point2D[][] projPoints = new Point2D[M][];
			for (int i = 0; i < M; i++) {
				projPoints[i] = camFinal.project(finalViews[i], modelPoints);
			}
			drawSquares(testIm.getStack(), projPoints);
			testIm.updateAndDraw();
		}
		*/
	}
	
	// --------------------------------------------------------------------
	
	/**
	 * Draws the array of image points to a given (possibly empty) stack image.
	 * The image points are assumed to be the corners of the standard
	 * calibration model, i.e., 4 consecutive points form a projected square.
	 * @param stack a stack with M images (views)
	 * @param imagePoints a sequence of 2D point sets, one for each view
	 */
	private void drawSquares(ImageStack stack, Point2D[][] imagePoints) {
		final int width = stack.getWidth();
		final int height = stack.getHeight();
		int M = imagePoints.length;
		for (int i = 0; i < M; i++) {
			ImageProcessor ip;
			if (stack.getSize() > i) {	// use existing stack slice
				ip = stack.getProcessor(i + 1);
			}
			else {						// create and fill new slice
				ip = new ColorProcessor(width, height);
				if (BackGroundColor != null) {
					ip.setColor(BackGroundColor);
					ip.fill();
				}
				stack.addSlice("View" + i, ip);
			}
			GridPainter painter = new GridPainter(ip);
			painter.drawSquares(imagePoints[i]);
		}
	}
	
}
