package sonkd;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;

/**
 * Main.java TODO:
 * 
 * @author Kim Dinh Son Email:sonkdbk@gmail.com
 */

public class Main {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		// System.loadLibrary("opencv_java2410");
	}

	static Scalar Colors[] = { new Scalar(255, 0, 0), new Scalar(0, 255, 0),
			new Scalar(0, 0, 255), new Scalar(255, 255, 0),
			new Scalar(0, 255, 255), new Scalar(255, 0, 255),
			new Scalar(255, 127, 255), new Scalar(127, 0, 255),
			new Scalar(127, 0, 127) };

	final static int FRAME_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width/2;
	final static int FRAME_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height/2;
	final static double MIN_BLOB_AREA = 300;

	static Mat imag = null;
	public static Tracker tracker;

	public static void main(String[] args) throws InterruptedException {
		JFrame jFrame = new JFrame("MULTIPLE-TARGET TRACKING");
		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel vidpanel = new JLabel();
		jFrame.setContentPane(vidpanel);
		jFrame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		jFrame.setLocation((3/4)* Toolkit.getDefaultToolkit().getScreenSize().width, (3/4)* Toolkit.getDefaultToolkit().getScreenSize().height);
		jFrame.setVisible(true);

		// ////////////////////////////////////////////////////////
		JFrame jFrame2 = new JFrame("MULTIPLE-TARGET TRACKING");
		jFrame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel vidpanel2 = new JLabel();
		jFrame2.setContentPane(vidpanel2);
		jFrame2.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		jFrame2.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width/2, (3/4)* Toolkit.getDefaultToolkit().getScreenSize().height);
		jFrame2.setVisible(true);
		// ////////////////////////////////////////////////////////

		Mat frame = new Mat();
		Mat outbox = new Mat();
		Mat diffFrame = null;
		Vector<Rect> array = new Vector<Rect>();

		BackgroundSubtractorMOG2 mBGSub = Video
				.createBackgroundSubtractorMOG2();

		tracker = new Tracker((float) 0.2, (float) 0.5, 100.0, 10, 10);
		
		// Thread.sleep(1000);
		VideoCapture camera = new VideoCapture();
		// camera.open("H:/VIDEO/Footage/Crowd_PETS09/S2/L2/Time_14-55/View_001/frame_%04d.jpg");
		camera.open("visiontraffic.avi");
		// camera.open("H:/VIDEO/Footage/TrackingBugs.mp4");
		//VideoCapture camera = new VideoCapture(0);
		int i = 0;

		if (!camera.isOpened()) {
			System.out.print("Can not open Camera, try it later.");
			return;
		}

		while (true) {
			if (!camera.read(frame))
				break;
			imag = frame.clone();
			//Imgproc.resize(frame, frame, new Size(FRAME_WIDTH, FRAME_HEIGHT));
			if (i == 0) {
				jFrame.setSize(frame.width(), frame.height());
				diffFrame = new Mat(outbox.size(), CvType.CV_8UC1);
				diffFrame = outbox.clone();
			}

			if (i == 1) {
				diffFrame = new Mat(frame.size(), CvType.CV_8UC1);
				processFrame(camera, frame, diffFrame, mBGSub);
				Imgproc.threshold(diffFrame, diffFrame, 127, 255,
						Imgproc.THRESH_BINARY);
				frame = diffFrame.clone();

				array = detectionContours(diffFrame);
				if (array.size() > 0) {
					// //////////////////////////////////////////////////////////////////					
					tracker.update(array, imag);
					for (int k = 0; k < tracker.tracks.size(); k++) {
						int traceNum = tracker.tracks.get(k).trace.size();
						if (traceNum > 1) {
							for (int jt = 1; jt < tracker.tracks.get(k).trace
									.size(); jt++) {
								Imgproc.line(
										imag,
										tracker.tracks.get(k).trace.get(jt - 1),
										tracker.tracks.get(k).trace.get(jt),
										Colors[tracker.tracks.get(k).track_id % 9],
										2, 4, 0);
							}
						}
					}
					
				}
			}

			i = 1;

			ImageIcon image = new ImageIcon(Mat2bufferedImage(imag));
			vidpanel.setIcon(image);
			vidpanel.repaint();
			// temponFrame = outerBox.clone();

			ImageIcon image2 = new ImageIcon(Mat2bufferedImage(frame));
			vidpanel2.setIcon(image2);
			vidpanel2.repaint();
		}

	}

	// background substraction
	protected static void processFrame(VideoCapture capture, Mat mRgba,
			Mat mFGMask, BackgroundSubtractorMOG2 mBGSub) {
		// capture.retrieve(mRgba, Imgproc.COLOR_BGR2RGB);
		// GREY_FRAME also works and exhibits better performance		
		mBGSub.apply(mRgba, mFGMask, 0.001);
		Imgproc.cvtColor(mFGMask, mRgba, Imgproc.COLOR_GRAY2BGRA, 0);
		Mat openElem = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
				new Size(5, 5), new Point(2, 2));
		Mat closeElem = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
				new Size(7, 7), new Point(3, 3));

		Imgproc.morphologyEx(mFGMask, mFGMask, Imgproc.MORPH_OPEN, openElem);
		Imgproc.morphologyEx(mFGMask, mFGMask, Imgproc.MORPH_CLOSE, closeElem);
	}

	private static BufferedImage Mat2bufferedImage(Mat image) {
		MatOfByte bytemat = new MatOfByte();
		Imgcodecs.imencode(".jpg", image, bytemat);
		byte[] bytes = bytemat.toArray();
		InputStream in = new ByteArrayInputStream(bytes);
		BufferedImage img = null;
		try {
			img = ImageIO.read(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return img;
	}

	public static Vector<Rect> detectionContours(Mat outmat) {
		Mat v = new Mat();
		Mat vv = outmat.clone();
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(vv, contours, v, Imgproc.RETR_LIST,
				Imgproc.CHAIN_APPROX_SIMPLE);

		int maxAreaIdx = -1;
		Rect r = null;
		Vector<Rect> rect_array = new Vector<Rect>();

		for (int idx = 0; idx < contours.size(); idx++) {
			Mat contour = contours.get(idx);
			double contourarea = Imgproc.contourArea(contour);
			if (contourarea > MIN_BLOB_AREA) {
				// MIN_BLOB_AREA = contourarea;
				maxAreaIdx = idx;
				r = Imgproc.boundingRect(contours.get(maxAreaIdx));
				rect_array.add(r);
				Imgproc.drawContours(imag, contours, maxAreaIdx, new Scalar(255, 255, 255));
			}

		}

		v.release();
		return rect_array;
	}

}
