package com.company.calibration_demos;


import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.pub.corners.Corner;
import imagingbook.pub.corners.HarrisCornerDetector;

import javax.imageio.ImageIO;
import javax.transaction.xa.Xid;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.lang.System.*;

import static java.lang.System.out;

public class Main {

    public static void main(String[] args) throws IOException {

        String path = "images/CameraCalibration/2017-06-01-03-11-44.png";
        BufferedImage colorImage = ImageIO.read(new File(path));

        BufferedImage image = new BufferedImage(colorImage.getWidth(), colorImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = image.getGraphics();
        g.drawImage(colorImage, 0, 0, null);
        g.dispose();

        ByteProcessor processor = new ByteProcessor(image);
        HarrisCornerDetector.Parameters parameters = new HarrisCornerDetector.Parameters();
        HarrisCornerDetector dectector =  new HarrisCornerDetector(processor);


        List<Corner> corners = dectector.findCorners();
        float averagex = 0.0f;
        float averagey = 0.0f;
        int nb = 0;
        for (Corner corner: corners) {
            float yFixed = image.getHeight() - corner.getY();
            averagex += corner.getX();
            averagey += yFixed;
            nb++;
        }

        averagex = averagex / nb;
        averagey = averagey / nb;

        out.println("Average " + averagex + " " + averagey);


        for (Corner corner: corners) {
            float yFixed = image.getHeight() - corner.getY();
            float Xdiff = (corner.getX()) - averagex;
            float Ydiff = yFixed - averagey;
            double distance = Math.sqrt(Xdiff * Xdiff + Ydiff * Ydiff);
            out.println("" + corner.getX() + "\t" + yFixed + "\t" + distance);
        }
        // write your code here
    }
}
