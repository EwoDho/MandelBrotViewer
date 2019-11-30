package be.kuleuven.mandelbrot;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class MandelBrotViewerJPPF extends JFrame {


    // frame related stuff
    private int width, height;
    private JPanel imagePanel;
    private JToolBar toolBar;
    private JLabel statusLabel, calculationTimeLabel;
    private JButton renderButton, zoomOutButton;
    private JProgressBar progressBar;
    private JTextField superSamplesInput;
    private JTextField maxIterationsInput;

    private BufferedImage image;
    private Stack<Rectangle2D.Double> viewPortStack;
    private Rectangle2D.Double viewPort, origViewPort;

    // mouse related stuff
    private boolean mouseDown;
    private Point mouseDownPoint, mouseDragPoint;

    // algorithm related stuff
    int superSamples = 1;
    int maxIterations = 1000;
    SwingWorker<Void, Void> swingWorker;

    private JPPFClient jppfClient;

    public MandelBrotViewerJPPF(int width, int height) {
        this.width = width;
        this.height = height;

        createFrame();
        clearImage();

        jppfClient = new JPPFClient();
    }

    private void createFrame() {

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exit = new JMenuItem("Exit");
        exit.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        exit.setText("Exit");
        fileMenu.add(exit);
        menuBar.add(fileMenu);

        origViewPort = new Rectangle2D.Double(-2.5, -1, 3.5, 2);
        viewPort = new Rectangle2D.Double(origViewPort.x, origViewPort.y, origViewPort.width, origViewPort.height);
        viewPortStack = new Stack<>();

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        imagePanel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.drawImage(image, 0, 0, null);

                if (mouseDown) {
                    g2.setColor(Color.RED);

                    int x = Math.min(mouseDownPoint.x, mouseDragPoint.x);
                    int y = Math.min(mouseDownPoint.y, mouseDragPoint.y);
                    int w = Math.abs(mouseDownPoint.x - mouseDragPoint.x);
                    int h = Math.abs(mouseDownPoint.y - mouseDragPoint.y);

                    g2.drawRect(x, y, w, h);

                }
            }
        };
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseDown = true;
                int x = e.getX();
                int y = e.getY();
                mouseDownPoint = new Point(x, y);
                System.out.printf("Mouse down: %b %s\n", mouseDown, mouseDownPoint);

            }


            @Override
            public void mouseMoved(MouseEvent e) {

                double nx = 1.0 * e.getX() / width;
                double ny = 1.0 * e.getY() / height;


                double vx = viewPort.getMinX() + nx * viewPort.getWidth();
                double vy = viewPort.getMaxY() - ny * viewPort.getHeight();

                statusLabel.setText(String.format("x: %f \t y: %f", vx, vy));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (mouseDown) {
                    int x = e.getX();
                    int y = e.getY();
                    mouseDragPoint = new Point(x, y);
                    imagePanel.repaint();
                }
                double nx = 1.0 * e.getX() / width;
                double ny = 1.0 * e.getY() / height;

                double vx = viewPort.getMinX() + nx * viewPort.getWidth();
                double vy = viewPort.getMaxY() - ny * viewPort.getHeight();

                statusLabel.setText(String.format("x: %f \t y: %f", vx, vy));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseDown = false;
                mouseDragPoint = new Point(e.getX(), e.getY());
                System.out.printf("Mouse up: %b %s\n", mouseDown, mouseDragPoint);

                int x = Math.min(mouseDownPoint.x, mouseDragPoint.x);
                int y = Math.min(mouseDownPoint.y, mouseDragPoint.y);
                int w = Math.abs(mouseDownPoint.x - mouseDragPoint.x);
                int h = Math.abs(mouseDownPoint.y - mouseDragPoint.y);

                double nx = 1.0 * x / width;
                double ny = 1.0 * y / height;
                double nx2 = 1.0 * (x + w) / width;
                double ny2 = 1.0 * (y + h) / height;


                double vx = viewPort.getMinX() + nx * viewPort.getWidth();
                double vy = viewPort.getMaxY() - ny * viewPort.getHeight();

                double vx2 = viewPort.getMinX() + nx2 * viewPort.getWidth();
                double vy2 = viewPort.getMaxY() - ny2 * viewPort.getHeight();

                if (swingWorker != null)
                    return;

                viewPortStack.push(viewPort);
                viewPort = new Rectangle2D.Double(Math.min(vx, vx2), Math.min(vy, vy2), Math.abs(vx2 - vx), Math.abs(vy2 - vy));
                launchCalculation();
            }

        };
        imagePanel.addMouseListener(mouseAdapter);
        imagePanel.addMouseMotionListener(mouseAdapter);
        imagePanel.addMouseWheelListener(mouseAdapter);
        imagePanel.setMinimumSize(new Dimension(width, height));
        imagePanel.setPreferredSize(new Dimension(width, height));

        toolBar = new JToolBar();
        createToolBar();


        statusLabel = new JLabel(String.format("x: \ty:"));
        this.setJMenuBar(menuBar);
        this.getContentPane().add(toolBar, BorderLayout.PAGE_START);
        this.getContentPane().add(imagePanel, BorderLayout.CENTER);
        this.getContentPane().add(statusLabel, BorderLayout.PAGE_END);
    }

    private void createToolBar() {
        toolBar.setFloatable(false);

        renderButton = new JButton();
        renderButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launchCalculation();
            }
        });
        renderButton.setText("Render Mandelbrot set");
        toolBar.add(renderButton);
        toolBar.addSeparator();

        zoomOutButton = new JButton();
        zoomOutButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!viewPortStack.empty()) {
                    viewPort = viewPortStack.pop();
                    launchCalculation();
                }
            }
        });
        zoomOutButton.setText("Zoom out");
        zoomOutButton.setEnabled(false);
        toolBar.add(zoomOutButton);
        toolBar.addSeparator();

        toolBar.add(new JLabel("\tEscape time alg. iterations:\t"));
        toolBar.addSeparator();

        maxIterationsInput = new JTextField("1000");
        maxIterationsInput.setHorizontalAlignment(JTextField.CENTER);
        toolBar.addSeparator();

        toolBar.add(maxIterationsInput);
        toolBar.addSeparator();

        toolBar.add(new JLabel(("\tSuper samples:\t")));
        toolBar.addSeparator();

        superSamplesInput = new JTextField("1");
        superSamplesInput.setHorizontalAlignment(JTextField.CENTER);
        toolBar.add(superSamplesInput);
        toolBar.addSeparator();

        progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        progressBar.setEnabled(false);
        toolBar.add(progressBar);
        toolBar.addSeparator();

        calculationTimeLabel = new JLabel("\tLast calculation time:\t0ms");
        toolBar.add(calculationTimeLabel);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    private void launchCalculation() {

        try {
            superSamples = Integer.parseUnsignedInt(superSamplesInput.getText().trim());
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Super samples needs to be integer and 0 <  and <= 32");
            return;
        }
        if (superSamples < 1) {
            superSamplesInput.setText("1");
            superSamples = 1;
        }
        if (superSamples > 32) {
            superSamplesInput.setText("32");
            superSamples = 32;
        }

        try {
            maxIterations = Integer.parseUnsignedInt(maxIterationsInput.getText().trim());
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "MaxIterations needs to be integer and 100 <=  and <= 5000");
            return;
        }
        if (maxIterations < 100) {
            maxIterationsInput.setText("100");
            maxIterations = 100;
        }
        if (maxIterations > 5000) {
            maxIterationsInput.setText("5000");
            maxIterations = 5000;
        }


        // launch swingworker
        final long startTime = System.currentTimeMillis();
        if (swingWorker != null) {
            swingWorker.cancel(true);
        }
        swingWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Random rnd = new Random();
                JPPFJob job = new JPPFJob();

                // TODO - voeg taken toe aan de Job
                // bvb.  job.add(MijnTaakObject)
                try {
                    for (int w = 0; w < width; w++) {
                        //setProgress((int) (100.0 * w / width));

                        RunTask task = new RunTask(maxIterations, superSamples, viewPort.getMinX(), viewPort.getMaxY(), viewPort.getWidth(), viewPort.getHeight(), w, width, height);
                        job.add(task);
                        // submit de job


                    }
                    System.out.println("Submitted async job, waiting for results");
                    job = jppfClient.submitAsync(job);
                    // wachten op het resultaat
                    int lastExecutedTaskCount = 0;
                    while (job.executedTaskCount() < job.getTaskCount()) {
                        if (job.executedTaskCount() > lastExecutedTaskCount) {
                            setProgress((int) (100.0 * job.executedTaskCount() / job.getTaskCount()));

                            //System.out.printf("Executed %d of %d tasks.\n", job.executedTaskCount(), job.getTaskCount());
                            lastExecutedTaskCount = job.executedTaskCount();
                        }
                        Thread.sleep(50);
                    }

                    System.out.println("Received all results");

                    // Todo - verwerk het resultaat (stel de rgb waarden in op de 'image')

                    int w = 0;
                    for (Task<?> allResult : job.getAllResults()) {
                        RunTask rT = (RunTask) allResult;
                        for (int i = 0; i < height; i++) {
                            //System.out.println(rT.getResult()[i][1]);
                            image.setRGB(w, i, new Color(rT.getResult()[i][0], rT.getResult()[i][1], rT.getResult()[i][2]).getRGB());
                        }

                        w++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }



                return null;
            }


            @Override
            protected void done() {
                setProgress(100);
                imagePanel.repaint();
                renderButton.setEnabled(true);
                if (!viewPortStack.empty()) {
                    zoomOutButton.setEnabled(true);
                }
                progressBar.setEnabled(false);

                long endTime = System.currentTimeMillis();
                calculationTimeLabel.setText(String.format("\tLast calculation time:\t%d ms", endTime - startTime));

            }


        };
        progressBar.setEnabled(true);
        renderButton.setEnabled(false);
        zoomOutButton.setEnabled(false);
        swingWorker.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("progress")) {
                    progressBar.setValue((Integer) evt.getNewValue());

                }

                if (!evt.getOldValue().equals(evt.getNewValue())) {
                    imagePanel.repaint();
                }
            }
        });
        swingWorker.execute();
        swingWorker = null;
    }


    private void clearImage() {

        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                image.setRGB(w, h, 0xffffffff);
            }
        }
    }

    public static void main(String... args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }

                MandelBrotViewerJPPF viewer = new MandelBrotViewerJPPF(1024, 768);
                viewer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                viewer.setVisible(true);
                viewer.pack();
                viewer.setResizable(false);
            }
        });

    }
}