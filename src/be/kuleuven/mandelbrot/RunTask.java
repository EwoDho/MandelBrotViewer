package be.kuleuven.mandelbrot;

import com.sun.prism.image.ViewPort;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.node.protocol.Task;

import java.awt.*;
import java.util.Random;

public class RunTask extends AbstractTask<float[][]> {
    public int maxIterations, superSamples, w, h, width, height;
    public double getMinX, getMaxY, getWidth, getHeight;
    public Random rnd;

    public RunTask(int maxIterations, int superSamples, double getMinX, double getMaxY, double getWidth, double getHeight,  int w,int width, int height,Random rnd) {
        this.maxIterations = maxIterations;
        this.superSamples = superSamples;
        this.getMinX = getMinX;
        this.getMaxY = getMaxY;
        this.getWidth = getWidth;
        this.getHeight = getHeight;
        this.w = w;
        this.h = h;
        this.height = height;
        this.width = width;
        this.rnd=rnd;

    }

    @Override
    public void run() {
        super.run();
        float r,g,b;
        float[][] arrh=new float[height][3];
        for(int i=0;i<height;i++) {
            r = 0; g = 0; b = 0;
            for (int sample = 0; sample < superSamples; sample++) {

                // escape time algorithm
                double x0, y0;
                if (superSamples == 1) {
                    x0 = getMinX + (w + .5) / width * getWidth;
                    y0 = getMaxY - (h + 0.5) / height * getHeight;
                } else {
                    x0 = getMinX + (w + rnd.nextDouble()) / width * getWidth;
                    y0 = getMaxY - (h + rnd.nextDouble()) / height * getHeight;
                }
                double x = 0;
                double y = 0;

                long iteration = 0;
                long max_iteration = maxIterations;

                while (x * x + y * y < 4 && iteration < max_iteration) {
                    double xtemp = x * x - y * y + x0;
                    y = 2 * x * y + y0;
                    x = xtemp;
                    iteration++;
                }
                // determine the color
                if (iteration < max_iteration) {

                    double quotient = (double) iteration / (double) max_iteration;
                    float c = (float) Math.pow(quotient, 1.0 / 3);
                    if (quotient > 0.5) {
                        // Close to the mandelbrot set the color changes from green to white
                        r += c;
                        g += 1.f;
                        b += c;
                    } else {
                        // Far away it changes from black to green
                        g += c;
                    }

                }
            }





            arrh[h][0] =  (r / superSamples);
            arrh[h][1]= (g / superSamples);
            arrh[h][2]=  (b / superSamples);
        }
        setResult(arrh);
    }
}