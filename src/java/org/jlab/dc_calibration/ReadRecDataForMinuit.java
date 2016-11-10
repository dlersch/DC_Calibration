
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.org.jlab.dc_calibration;

//http://stackoverflow.com/questions/4412020/using-abs-method-in-java-my-compiler-doesnt-know-the-method
import static java.lang.Math.abs;
//import static org.kp.myLib1.KpLib1.print;
//import static org.kp.myLib1.KpLib1.System.out.println;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.freehep.math.minuit.FCNBase;
import org.freehep.math.minuit.FunctionMinimum;
import org.freehep.math.minuit.MnMigrad;
import org.freehep.math.minuit.MnStrategy;
import org.freehep.math.minuit.MnUserParameters;
import org.jlab.groot.base.TStyle;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.io.evio.EvioDataBank;
import org.jlab.io.evio.EvioDataChain;
import org.jlab.io.evio.EvioDataEvent;

/**
 *
 * @author KPAdhikari
 */
public class ReadRecDataForMinuit implements ActionListener {
	int eventNr;
	// static boolean debug = false; // for debugging
	static int debug = -1;// 0; // for debugging
	protected static java.util.List<String> inputFiles;
	static long numEvents = 15000;// (long) 1e9; // events to process
	static long printEventNr = 20000; // display progress

	// Global variables accessable from other classes as well
	// http://stackoverflow.com/questions/19939769/how-do-i-create-a-global-variable-in-java-such-that-all-classes-can-access-it
	public static final int nSupLayers = 2;
	public static final int nThBinsVz = 6; // [nThBinsVZ][2]
	// double thEdgeVz[][] = { {-2.0, 2.0}, {8.0, 12.0}, {18.0, 22.0}, {28.0,
	// 32.0}, {38.0, 42.0}, {48.0, 52.0}};
	public static final double thEdgeVzL[] = { -2.0, 8.0, 18.0, 28.0, 38.0, 48.0 };
	public static final double thEdgeVzH[] = { 2.0, 12.0, 22.0, 32.0, 42.0, 52.0 };
	public static final double[] wpdist = { 0.3861, 0.4042, 0.6219, 0.6586, 0.9351, 0.9780 };
	public static final double rad2deg = 57.2957795130823229;
	public static final double deg2rad = 1.74532925199432955e-02;// =1.0/rad2deg;
																	// //ROOT:
																	// 180.0/TMath::Pi()
	public static final double cos30 = Math.cos(30.0 / rad2deg);
	public static final double beta = 1.0; // It has to be made
											// particle/momentum dependent later
											// on
	// ===== 8/11/16
	public static final int nFreePars = 5;
	public static final String parName[] = { "v0", "deltamn", "tmax1", "tmax2", "distbeta" };
	public static double prevFitPars[] = { 62.92e-04, 1.35, 137.67, 148.02, 0.055 };// Later
																					// it
																					// will
																					// be
																					// read
																					// from
																					// a
																					// results
																					// file
																					// from
																					// prev.
																					// fit.
	// ===== 8/11/16

	private int x = 0, y = 0;

	public ReadRecDataForMinuit() {
		this.x = 3;
		this.y = 5;// Just for a quick test
	}

	public void actionPerformed(ActionEvent ev) {
		/*
		 * //label2.setText("Label2: This time you can see more words here.");
		 * if (y == 0) {
		 * label.setText("Label2: This time you can see more words here."); y =
		 * 1; } else if (y == 1) { label.setText(""); y = 0; }
		 */
		String myMessage = null;
		myMessage = String.format("Hello from Dialog inside TestEvent class!! x=%d, y=%d", x, y);

		JFrame frame = new JFrame("JOptionPane showMessageDialog example1");

		// show a joptionpane dialog using showMessageDialog
		// JOptionPane.showMessageDialog(frame, myMessage);
		JOptionPane.showMessageDialog(frame, "Click OK to start reading the reconstructed file ...");
		try {
			processData();
		} catch (IOException ex) {
			Logger.getLogger(ReadRecDataForMinuit.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void processData() throws IOException {
		long printEvent;
		if (debug == 1) {
			printEvent = 1;
		} else {
			printEvent = printEventNr;
		}

		int inFileNum = 1;
		String inputFile = null;
		String fDir = "C:\\Users\\KPAdhikari\\Desktop\\BigFls\\CLAS12";
		String fName = "reconstructedDataR128T0corT2DfromCCDBvarFit10";
		EvioDataChain reader = new EvioDataChain();
		// for (String inputFile : inputFiles) {
		for (int fN = 0; fN < inFileNum; fN++) {
			// if (inputFile == null) { break; }
			inputFile = String.format("%s\\%s.%d.evio", fDir, fName, fN);
			reader.addFile(inputFile);
		}
		reader.open();
		System.out.println("Opened the input data file (from ReadRecDataIn class) !");

		int counter = 0, NumEv2process = 20, nTBHits = 0;
		EvioDataBank bnkHits = null;

		/////////////// Start of a small test event loop within processData()
		/////////////// ////////////////
		// Now loop over all the events
		while (reader.hasEvent()) {
			EvioDataEvent event = reader.getNextEvent();
			boolean tbHits = false;
			// if(counter<NumEv2process)
			if (counter < 20) {
				if (event.hasBank("TimeBasedTrkg::TBHits")) {
					bnkHits = (EvioDataBank) event.getBank("TimeBasedTrkg::TBHits");
					tbHits = true;
					nTBHits = bnkHits.rows();
					System.out.println("# of hist in this " + counter + "th event = " + nTBHits);
				}
			} else
				break;
			counter++;
		}

		/////////////// End of a small test event loop within processData()
		/////////////// //////////////////

		// System.out.println("Hello world!!! How is it going.!!");
		System.out.println("Hello world!!! I am a class with name 'readDataInFromReconstructedEvioOutput'!!");

		String txtOp = "TxtOp/opDoca_Time.txt";

		/*
		 * //if(args.size()<5)//<2) if(args.length<5)//<2) {
		 * System.out.println("Usage:"); //System.out.println(
		 * "./bin/run-groovy   thisFile.groovy  inputFile.evio  NumberOfEventsToProcess dataType "
		 * // + " parNum" + " parValue" + " debug"); System.out.println(
		 * "./bin/run-groovy   thisFile.groovy  inputFileNameWithout_N.evio HowManyInputFiles NumberOfEventsToProcess dataType debug"
		 * ); System.out.println(
		 * "inputFileNameWithout_N.evio: if we have input files input.0.evio, input.1.evio, ..., input.N.evio then"
		 * ); System.out.println(
		 * "\t inputFileNameWithout_N.evio=input and  HowManyInputFiles=N");
		 * System.out.println(
		 * "To process all events, please give -1 for the last or 3rd argument."
		 * ); System.out.println("dataType: Cosmic or Gemc");
		 * //System.out.println(
		 * "Don't forget to append .png in the image name for opImageNm.");
		 * System.out.println(
		 * "If debug==-1, it will turn off printout from each event, to make it run faster."
		 * ); System.out.println(
		 * "\t if debug==-2, print the remaining few spiky events.");
		 * System.out.println(
		 * "\t if debug==-3, print trkDoca & time info into a text output file "
		 * + txtOp); return; } //==========10/12/15 String inputFile = args[0];
		 * int inFileNum = Integer.parseInt(args[1]); int NumEv2process =
		 * Integer.parseInt(args[2]); String dataType =
		 * args[3];//outputImageName = args[2]; int debug =
		 * Integer.parseInt(args[4]); //==========10/12/15
		 */
		inFileNum = 1;
		NumEv2process = 20000;
		String dataType = "Cosmic";// 10/27/16

		System.out.println("inputFile: " + inputFile);
		System.out.println("inFileNum, NumEv2process, dataType, debug: " + inFileNum + " " + NumEv2process + " "
				+ dataType + " " + debug);

		/*
		 * inputFile = String.format("%s.0.evio",args[0]); EvioSource reader =
		 * new EvioSource(); //new EvioReader();
		 * reader.open(inputFile);//open("myfile.evio"); //==========10/12/15
		 * int totNumOfEvs = reader.getSize();
		 * System.out.println("Total and ToBeProcessed Events: "+totNumOfEvs+
		 * ", " +NumEv2process); if(NumEv2process<1) NumEv2process=totNumOfEvs;
		 * //==========10/12/15
		 */

		double dMax = 0.0;
		System.out.print("wpdist[] = {");
		for (int i = 0; i < wpdist.length; i++) {
			System.out.print(wpdist[i] + ",");
		}
		System.out.println("}");

		/*
		 * ========= From Veronique's email dated 4/18/16: ========= The code
		 * now creates 2 new banks HitBasedTrkg::HBSegmentTrajectory and
		 * TimeBasedTrkg::TBSegmentTrajectory which contain the following
		 * columns:
		 * 
		 * segmentID --> the ID of the segment on trajectory sector superlayer
		 * layer --> the layer from 1 to 6 in the superlayer on trajectory
		 * matchedHitID --> the ID of the hit that is on trajectory (allowing a
		 * max of 2 cells off) trkDoca --> the track doca at a given layer
		 * calculated from the trajectory.
		 * 
		 * The layer is efficient if matchedHitID is not -1. For a given layer,
		 * if you fill a histogram hTot with trkDoca for all values of
		 * matchedHitID and fill a histogram hE with trkDoca for values of
		 * matchedHitID!=1, and get the ratio of hE to HTot you will get the
		 * efficiency as a function of trkDoca.
		 */
		counter = 0;
		int counter2 = 0;
		int id = -1, sector = -1, superlayer = -1, SL = -1, layer = -1, wire = -1, LR = -1, size = -1;// ,
																										// clusterID;
		int segmentID = -100, matchedHitID = -100;
		double trkDoca = 0.0, NtrkDoca = 0.0, docaMax = 0.0, doca = 0.0, docaNorm = 0.0, time = 0.0, X = 0.0, Z = 0.0,
				timeResidual = 0.0, docaError = 0.0, fitChisqProb = 0.0;
		int ID = 0, clusterID = 0;
		double avgWire = 0.0, fitSlope = 0.0, thDeg = 0.0, thDeg2 = 0.0;// For
																		// variables
																		// in
																		// TBSegments
																		// bankx

		// ka: 5/12/16: I think Veronique's hit-pruning algorithm doesn't allow
		// more than 12 hits per segment.
		// and if there is less than 12 hits involved, then the rest of hitID[]
		// elements will be givne a
		// a value of -1. For example, lets say a segment has only 9 hits
		// involded, then hit10/11/12 will
		// have an ID of -1 (not a real hit ID).
		int[] hitID = { -22, -22, -22, -22, -22, -22, -22, -22, -22, -22, -22, -22 };
		// int[] hitID = new int[12];//This also worked (in place of above line)

		// ===========================
		// Arrays to store TBhits bank-variables to access easily to relate to
		// other banks
		int[] gLayer = new int[5000], gWire = new int[5000]; // big # not to let
																// really noisy
																// events crash
																// the program
		double[] gTime = new double[5000], gDoca = new double[5000], gTrkDoca = new double[5000],
				gTimeRes = new double[5000];
		double[] gX = new double[5000], gZ = new double[5000];
		int[] gSegmClustID = new int[50000]; // allowing for a maximum of 50
												// segments in an event, noticed
												// cluster ID as big as 10001
		double[] gSegmAvgWire = new double[50000], gFitChisqProb = new double[50000], gSegmTh = new double[50000];
		int[] gSegmThBin = new int[50000];

		// TStyle style;
		TStyle.createAttributes(); // TStyle.setFrameFillColor(215,235,245);//TStyle.setFrameFillColor(160,215,230);
		// TStyle.setAxisFont("Helvetica",24);//TStyle.setAxisColor(255,255,255);

		int nSL = 2, nLayer = 6;
		// ============= 4/18/16 ============= trkDoca histos for each layer in
		// a superlayer (so 6*2 histos)
		double[] docaBins = { -0.8, -0.6, -0.4, -0.2, -0.0, 0.2, 0.4, 0.6, 0.8 };
		int nHists = 8, docaBin = -1;// 8 DocaBins
		int nWireBins = 120;
		double wMn = -1.0, wMx = 119.0;
		String hNm = "", hTtl = "";// It seems variables must be initialized in
									// Java to avoid compilation errors
		// =============== New and perhaps easier/better Idea from Will Phelps
		// =====
		H1F[][][] hArrWire = new H1F[nSL][nLayer][nHists];
		hArrWire[0][0][0] = new H1F("wireDb0_0_0", 120, -1.0, 119.0);
		for (int i = 0; i < nSL; i++) {
			for (int j = 0; j < nLayer; j++) {
				for (int k = 0; k < nHists; k++) {
					hNm = String.format("wireS%dL%dDb%02d", i + 1, j + 1, k);
					hArrWire[i][j][k] = new H1F(hNm, 120, -1.0, 119.0);
					hTtl = String.format("wire (SL=%d, Layer%d, DocaBin=%02d)", i + 1, j + 1, k);
					hArrWire[i][j][k].setTitle(hTtl);
					hArrWire[i][j][k].setLineColor(i + 1);
				}
			}
		}

		H1F[] h1ThSL = new H1F[nSL];
		int thBn = -1, thBnVz = -1;
		h1ThSL[0] = new H1F("thetaSL1", 120, -60.0, 60.0);
		h1ThSL[0].setTitle("#theta");
		h1ThSL[0].setLineColor(1);
		h1ThSL[1] = new H1F("thetaSL2", 120, -60.0, 60.0);
		h1ThSL[1].setTitle("#theta");
		h1ThSL[1].setLineColor(2);
		// int nTh=9; def
		// thBins=[-60.0,-40.0,-20.0,-10.0,-1.0,1.0,10.0,20.0,40.0,60.0] as
		// double[];
		int nTh = 9;
		double[] thBins = { -60.0, -40.0, -20.0, -10.0, -1.0, 1.0, 10.0, 20.0, 40.0, 60.0 };
		H1F[][] h1timeSlTh = new H1F[nSL][nTh];
		for (int i = 0; i < nSL; i++) {
			for (int k = 0; k < nTh; k++) {
				hNm = String.format("timeSL%dThBn%d", i, k);
				h1timeSlTh[i][k] = new H1F(hNm, 200, -10.0, 190.0);
				hTtl = String.format("time (SL=%d, th(%.1f,%.1f)", i + 1, thBins[k], thBins[k + 1]);
				h1timeSlTh[i][k].setTitle(hTtl);
				h1timeSlTh[i][k].setLineColor(i + 1);
			}
		}

		// Histograms to get ineff. as fn of trkDoca (NtrkDoca =
		// trkDoca/docaMax)
		H1F[][] h1trkDoca2Dar = new H1F[nSL][3], h1NtrkDoca2Dar = new H1F[nSL][3], h1NtrkDocaP2Dar = new H1F[nSL][3];// [3]
																														// for
																														// all
																														// good
																														// hits,
																														// only
																														// bad
																														// (matchedHitID==-1),
																														// and
																														// ratio
		H1F[][][] h1trkDoca3Dar = new H1F[nSL][nLayer][3], h1NtrkDoca3Dar = new H1F[nSL][nLayer][3],
				h1NtrkDocaP3Dar = new H1F[nSL][nLayer][3];// [3] for all good
															// hits, only bad
															// (matchedHitID==-1),
															// and ratio
		H1F[][][][] h1trkDoca4Dar = new H1F[nSL][nLayer][nTh][3];// [3] for all
																	// good
																	// hits,
																	// only bad
																	// (matchedHitID==-1),
																	// and ratio
		H1F[][][][] h1wire4Dar = new H1F[nSL][nLayer][nTh][2], h1avgWire4Dar = new H1F[nSL][nLayer][nTh][2];// no
																											// ratio
																											// here
		H1F[][][][] h1fitChisqProbSeg4Dar = new H1F[nSL][nLayer][nTh][2];// no
																			// ratio
																			// here
		String[] hType = { "all hits", "matchedHitID==-1", "Ratio==Ineff." };// as
																				// String[];

		for (int i = 0; i < nSL; i++) {
			for (int k = 0; k < 3; k++) { // These are for histos integrated
											// over all layers
				hNm = String.format("trkDocaS%dH%d", i + 1, k);
				h1trkDoca2Dar[i][k] = new H1F(hNm, 90, -0.9, 0.9);
				hNm = String.format("NtrkDocaS%dH%d", i + 1, k);
				h1NtrkDoca2Dar[i][k] = new H1F(hNm, 120, -1.2, 1.2);
				hNm = String.format("NtrkDocaPS%dH%d", i + 1, k);
				h1NtrkDocaP2Dar[i][k] = new H1F(hNm, 120, 0.0, 1.2);
				if (k == 0)
					hTtl = String.format("all hits (SL=%d)", i + 1);
				if (k == 1)
					hTtl = String.format("matchedHitID==-1 (SL=%d)", i + 1);
				if (k == 2)
					hTtl = String.format("Ineff. (SL=%d)", i + 1);
				h1trkDoca2Dar[i][k].setTitle(hTtl);
				h1trkDoca2Dar[i][k].setLineColor(i + 1);
				h1NtrkDoca2Dar[i][k].setTitle(hTtl);
				h1NtrkDoca2Dar[i][k].setLineColor(i + 1);
				h1NtrkDocaP2Dar[i][k].setTitle(hTtl);
				h1NtrkDocaP2Dar[i][k].setLineColor(i + 1);
			}
			for (int j = 0; j < nLayer; j++) {
				for (int k = 0; k < 3; k++) { // These are for histos integrated
												// over all theta
					hNm = String.format("trkDocaS%dL%dH%d", i + 1, j + 1, k);
					h1trkDoca3Dar[i][j][k] = new H1F(hNm, 90, -0.9, 0.9);
					hNm = String.format("NtrkDocaS%dL%dH%d", i + 1, j + 1, k);
					h1NtrkDoca3Dar[i][j][k] = new H1F(hNm, 120, -1.2, 1.2);
					hNm = String.format("NtrkDocaPS%dL%dH%d", i + 1, j + 1, k);
					h1NtrkDocaP3Dar[i][j][k] = new H1F(hNm, 120, 0.0, 1.2);
					if (k == 0)
						hTtl = String.format("all hits (SL=%d, Layer%d)", i + 1, j + 1);
					if (k == 1)
						hTtl = String.format("matchedHitID==-1 (SL=%d, Layer%d)", i + 1, j + 1);
					if (k == 2)
						hTtl = String.format("Ineff. (SL=%d, Layer%d)", i + 1, j + 1);
					h1trkDoca3Dar[i][j][k].setTitle(hTtl);
					h1trkDoca3Dar[i][j][k].setLineColor(i + 1);
					h1NtrkDoca3Dar[i][j][k].setTitle(hTtl);
					h1NtrkDoca3Dar[i][j][k].setLineColor(i + 1);
					h1NtrkDocaP3Dar[i][j][k].setTitle(hTtl);
					h1NtrkDocaP3Dar[i][j][k].setLineColor(i + 1);
				}

				for (int th = 0; th < nTh; th++) {
					for (int k = 0; k < 3; k++) {
						hNm = String.format("trkDocaS%dL%dTh%02dH%d", i + 1, j + 1, th, k);
						h1trkDoca4Dar[i][j][th][k] = new H1F(hNm, 90, -0.9, 0.9);

						if (k == 0)
							hTtl = String.format("all hits (SL=%d, Layer%d, th(%.1f,%.1f))", i + 1, j + 1, thBins[th],
									thBins[th + 1]);
						if (k == 1)
							hTtl = String.format("matchedHitID==-1 (SL=%d, Layer%d, th(%.1f,%.1f))", i + 1, j + 1,
									thBins[th], thBins[th + 1]);
						if (k == 2)
							hTtl = String.format("Ineff. (SL=%d, Layer%d, th(%.1f,%.1f))", i + 1, j + 1, thBins[th],
									thBins[th + 1]);
						h1trkDoca4Dar[i][j][th][k].setTitle(hTtl);
						h1trkDoca4Dar[i][j][th][k].setLineColor(i + 1);
					}
					for (int k = 0; k < 2; k++) {
						hNm = String.format("wireS%dL%dTh%02dH%d", i + 1, j + 1, th, k);
						h1wire4Dar[i][j][th][k] = new H1F(hNm, 120, -1.0, 119.0);
						hTtl = String.format("wire # for %s (SL=%d, Lay%d, th(%.1f,%.1f))", hType[k], i + 1, j + 1,
								thBins[th], thBins[th + 1]);
						h1wire4Dar[i][j][th][k].setTitle(hTtl);
						h1wire4Dar[i][j][th][k].setLineColor(i + 1);
						hNm = String.format("avgWireS%dL%dTh%02dH%d", i + 1, j + 1, th, k);
						h1avgWire4Dar[i][j][th][k] = new H1F(hNm, 120, -1.0, 119.0);
						hTtl = String.format("avgWire(SegBnk) for %s (SL=%d, Lay%d, th(%.1f,%.1f))", hType[k], i + 1,
								j + 1, thBins[th], thBins[th + 1]);
						h1avgWire4Dar[i][j][th][k].setTitle(hTtl);
						h1avgWire4Dar[i][j][th][k].setLineColor(i + 1);
						hNm = String.format("fitChisqProbS%dL%dTh%02dH%d", i + 1, j + 1, th, k);
						h1fitChisqProbSeg4Dar[i][j][th][k] = new H1F(hNm, 90, -0.1, 0.1);
						hTtl = String.format("fitChisqProbSeg(SegBnk) for %s (SL=%d, Lay%d, th(%.1f,%.1f))", hType[k],
								i + 1, j + 1, thBins[th], thBins[th + 1]);
						h1fitChisqProbSeg4Dar[i][j][th][k].setTitle(hTtl);
						h1fitChisqProbSeg4Dar[i][j][th][k].setLineColor(i + 1);
					}
				}
			}
		}

		// =============== new Idea from Will Phelps =====
		H2F[][] h2timeVtrkDoca = new H2F[nSL][2]; // 2 for 2 theta bins 0, 30
		int[] thetaBins = { 0, 30 };// as int[];
		for (int i = 0; i < nSL; i++) {
			for (int j = 0; j < 2; j++) { // 2 theta bins +/-1 deg around 0 and
											// 30 deg
				hNm = String.format("timeVtrkDocaS%dTh%02d", i, j);
				h2timeVtrkDoca[i][j] = new H2F(hNm, 200, 0.0, 1.0, 150, 0.0, 200.0);
				hTtl = String.format("time vs |trkDoca| (SL=%d, th=%02d+/-1.0)", i + 1, thetaBins[j]);
				h2timeVtrkDoca[i][j].setTitle(hTtl); // h2timeVtrkDoca[i][j].setMarkerColor(2);
			}
		}

		/////// public static final int nThBinsVz = 6; //[nThBinsVZ][2]
		// double thEdgeVz[][] = { {-2.0, 2.0}, {8.0, 12.0}, {18.0, 22.0},
		/////// {28.0, 32.0}, {38.0, 42.0}, {48.0, 52.0}};
		/////// public static final double thEdgeVzL[] = { -2.0, 8.0, 18.0,
		/////// 28.0, 38.0, 48.0};
		/////// public static final double thEdgeVzH[] = { 2.0, 12.0, 22.0,
		/////// 32.0, 42.0, 52.0};
		H2F[][] h2timeVtrkDocaVZ = new H2F[nSL][nThBinsVz]; // 2 for 2 theta
															// bins 0, 30
		for (int i = 0; i < nSL; i++) {
			for (int j = 0; j < nThBinsVz; j++) { // nThBinsVZ theta bins +/-2
													// deg around 0, 10, 20, 30,
													// 40, and 50 degs
				hNm = String.format("timeVtrkDocaS%dTh%02d", i, j);
				h2timeVtrkDocaVZ[i][j] = new H2F(hNm, 200, 0.0, 1.0, 150, 0.0, 200.0);
				hTtl = String.format("time vs |trkDoca| (SL=%d, th(%2.1f,%2.1f))", i + 1, thEdgeVzL[j], thEdgeVzH[j]); // Worked
				// hTtl = String.format("time vs |trkDoca| (SL=%d,
				// th(%2.1f,%2.1f))",i+1,thEdgeVz[0][j],thEdgeVz[1][j]);
				// //Didn't work
				h2timeVtrkDocaVZ[i][j].setTitle(hTtl); // h2timeVtrkDoca[i][j].setMarkerColor(2);
			}
		}

		double[] pars4FitLine = { prevFitPars[0], prevFitPars[1], prevFitPars[2], prevFitPars[3], prevFitPars[4], 1.0,
				0.0, 0.3861 };
		for (int i = 0; i < nSL; i++) {
			for (int j = 0; j < nThBinsVz; j++) {
				pars4FitLine[5] = 1.0 * (i + 1);
				pars4FitLine[6] = 0.5 * (thEdgeVzL[j] + thEdgeVzH[j]);
				pars4FitLine[7] = 2.0 * wpdist[i];
			}
		}
		double prevFitVal = 0.0, deltaVal = 0.0;

		File file; // def writer;
		FileWriter fw;
		BufferedWriter bw;
		FileOutputStream fos;
		// http://grails.asia/groovy-file-examples
		if (debug == -3) {
			// if(args.size()<6) {System.out.println("Enter op-txt-file name
			// such as
			// TxtOp/opDoca_TimeRun128.txt as last arg."); return;}
			// if(args.length<6) {System.out.println("Enter op-txt-file name
			// such as
			// TxtOp/opDoca_TimeRun128.txt as last arg."); return;}
			// txtOp = args[5]; //file = new File(txtOp); file.delete();
			txtOp = "textOutput.txt";
			file = new File(txtOp); // ("C:/temp/test.txt") //Create a new file
			if (!file.exists()) {
				file.createNewFile();
			} // If file doesn't exist, create a new one.
			/*
			 * //fw = new FileWriter(file.getAbsoluteFile()); fw = new
			 * FileWriter(file); bw = new BufferedWriter(fw);
			 */
			fos = new FileOutputStream(file);
			bw = new BufferedWriter(new OutputStreamWriter(fos));

			// writer = file.newWriter; //("C:/temp/test.txt")
			// file.write "This is the first line\n";//worked
			// file << "This is the second line\n";//worked
			// System.out.println file.text;//worked
			// file << "This is the third line\n";//worked
			// file <<"SegID SL Layer hitID thetaDeg time_ns trkDoca_cm \n";
			// file.write("EvNum SegID SL Layer hitID thetaDeg time_ns
			// trkDoca_cm \n");
			// file.write("EvNum SegID SL Layer nTBHits nTBCrosses hitID
			// thetaDeg avgWire fitChisqProb time_ns trkDoca_cm timeResidual
			// (calc)doca\n");
			// bw.write("EvNum SegID SL Layer nTBHits nTBCrosses hitID thetaDeg
			// avgWire fitChisqProb time_ns trkDoca_cm timeResidual
			// (calc)doca\n");
			bw.write(
					"EvNum  SegID   SL   Layer  nTBHits  nTBCrosses   hitID   thetaDeg  avgWire  fitChisqProb  time_ns    trkDoca_cm  timeResidual  (calc)doca\n");
			// writer <<"SegID SL Layer hitID thetaDeg time_ns trkDoca_cm \n";
			// fos.close();
			bw.close();
		}

		Writer output;
		if (debug == -3) {
			// output = new BufferedWriter(new FileWriter(txtOp)); //clears file
			// every time
			// ou have to open the file in append mode, which can be achieved by
			// using the FileWriter(String fileName, boolean append)
			// constructor.
			output = new BufferedWriter(new FileWriter(txtOp, true)); // keeps
																		// previous
																		// content,
																		// and
																		// appends
																		// new
																		// line
			for (int i = 0; i < 4; i++) {
				output.append("New Line!" + i + "\n");
			}
			output.close();
			appendIt(txtOp, "Haha ha ...\n");
		}

		// if(debug==-3) output.close();
		if (debug == -3)
			appendIt(txtOp, "Haha ha ...2 \n");

		/*
		 * EvioDataChain reader = new EvioDataChain();
		 * //reader.addFile(inputFile); //reader.open(); for(int fN=0;
		 * fN<inFileNum;fN++) { inputFile =
		 * String.format("%s.%d.evio",args[0],fN); //inputFile =
		 * String.format("%s.%d.n.evio",args[0],fN); //temp
		 * System.out.println("Inputfile: " + inputFile);
		 * reader.addFile(inputFile); } reader.open();
		 * System.out.println("Opened " + inFileNum + " evio files.");
		 */

		// String oDir =
		// "/u/site/www/html/Hall-B/secure/clas12/adhikari/GemcCosmicComp/New1DcCosmic/LayerEff/";
		String oDir = "/u/site/www/html/Hall-B/secure/clas12/adhikari/DCcalib/Time2Dist/RDjava";
		oDir = String.format("C:\\Users\\KPAdhikari\\Desktop\\BigFls\\CLAS12\\T2DOp");
		// String outputFile =
		// String.format("%s/fitPars_residualInDocaBins_TBhits%s.txt",oDir,dataType);
		String outputFile = String.format("%s/fitPars_residualInDocaBins_TBhits%s.txt", oDir, dataType);
		String printIt;

		// http://grails.asia/groovy-file-examples
		// File file = new File(outputFile); //("C:/temp/test.txt") //worked
		// ##########def file = new File(outputFile); //("C:/temp/test.txt")

		// http://www.studytrails.com/java-io/file-copying-and-moving-deleting.jsp
		// ##########file.delete(); file = new File(outputFile); //Deleting and
		// opening again (to avoid appending to old file)

		// http://stackoverflow.com/questions/1016278/is-this-the-best-way-to-rewrite-the-content-of-a-file-in-java
		// FileWriter file = new FileWriter(fl, false); // true to append, //
		// false to overwrite.
		// file.write "This is the first line\n"; // works only with File, not
		// FileWriter
		// System.out.println file.text; // works only with File, not FileWriter
		// ########## file << "Superlayer docaBinNum GausFitPars(Amp Mean
		// Sigma)\n"; // works
		// file.write("Superlayer docaBinNum GausFitPars(Amp Mean Sigma)\n");
		int[] idAll, secAll, slAll, layAll, wireAll, lrAll, clustIDAll;
		double[] timeAll, docaAll, trkDocaAll, xAll, zAll, timeResAll, docaErrAll;

		// 8/10/16: without 'null' assignment, Compiler gave: "error: variable
		// bnkSegTrks might not have been initialized"
		bnkHits = null;
		EvioDataBank bnkClust = null, bnkSegs = null, bnkSegTrks = null, bnkCross = null;

		/////////////// Start of event loop within processData()
		/////////////// /////////////////////////////
		while (reader.hasEvent()) {
			EvioDataEvent event = reader.getNextEvent();
			// boolean tbHits=false, tbClust=true, tbSegs=true, tbSegTrks=false,
			// tbCross=false;//, tbClusts=false;
			boolean tbHits = false, tbClust = false, tbSegs = false, tbSegTrks = false, tbCross = false;// ,
																										// tbClusts=false;
			nTBHits = 0;
			int nTBClust = 0, nTBSegs = 0, nTBSegTrks = 0, nTBCrosses = 0;
			int nHitsInSeg = 0;
			if (NumEv2process > 0 && counter == NumEv2process)
				break; // 5/16/16

			// if(counter<NumEv2process)//<5)
			if (true) // Replacement for above line to make it work
			{
				// event.show(); // print out all banks in the event
				if (event.hasBank("TimeBasedTrkg::TBHits")) { // bnkHits.show();//printout
																// the content
																// of the bank
					bnkHits = (EvioDataBank) event.getBank("TimeBasedTrkg::TBHits");
					tbHits = true;
					nTBHits = bnkHits.rows();
				}
				if (event.hasBank("TimeBasedTrkg::TBClusters")) { // bnkHits.show();//printout
																	// the
																	// content
																	// of the
																	// bank
					bnkClust = (EvioDataBank) event.getBank("TimeBasedTrkg::TBClusters");
					tbClust = true;
					nTBClust = bnkClust.rows();
				}
				if (event.hasBank("TimeBasedTrkg::TBSegments")) {
					bnkSegs = (EvioDataBank) event.getBank("TimeBasedTrkg::TBSegments");
					tbSegs = true;
					nTBSegs = bnkSegs.rows();
				}
				if (event.hasBank("TimeBasedTrkg::TBSegmentTrajectory")) {
					bnkSegTrks = (EvioDataBank) event.getBank("TimeBasedTrkg::TBSegmentTrajectory");
					tbSegTrks = true;
					nTBSegTrks = bnkSegTrks.rows();
				}
				if (event.hasBank("TimeBasedTrkg::TBCrosses")) {
					bnkCross = (EvioDataBank) event.getBank("TimeBasedTrkg::TBCrosses");
					tbCross = true;
					nTBCrosses = bnkCross.rows();
				}
				if (debug > -1)
					System.out.println("###### ###### ###### nEvts nTBHits nTBClust nTBSegs nTBSegTrks nTBCrosses: "
							+ counter + ", " + nTBHits + ", " + nTBClust + ", " + nTBSegs + ", " + nTBSegTrks + ", "
							+ nTBCrosses);

				// double[] var_TBhits = new double[17]; //var_TBhits[0] =
				// (double) bank_TBhits.getInt("id",i);

				if (debug > -1)
					System.out.println("============ Entries from TBHits Bank:============ ");
				if (debug > -1)
					System.out.println(
							"hitID,sector,SL,L,Wire,LR,time,doca,trkDoca,X,Z,clusterID,timeResidual,docaError");
				if (tbHits == true) {
					idAll = new int[nTBHits];
					secAll = new int[nTBHits];
					slAll = new int[nTBHits];
					layAll = new int[nTBHits];
					wireAll = new int[nTBHits];
					lrAll = new int[nTBHits];

					timeAll = new double[nTBHits];
					docaAll = new double[nTBHits];
					trkDocaAll = new double[nTBHits];
					xAll = new double[nTBHits];
					zAll = new double[nTBHits];
					clustIDAll = new int[nTBHits];
					timeResAll = new double[nTBHits];
					docaErrAll = new double[nTBHits];
					for (int j = 0; j < nTBHits; j++) {
						id = bnkHits.getInt("id", j);
						idAll[j] = bnkHits.getInt("id", j);
						secAll[j] = bnkHits.getInt("sector", j);
						slAll[j] = bnkHits.getInt("superlayer", j);
						layAll[j] = bnkHits.getInt("layer", j);
						wireAll[j] = bnkHits.getInt("wire", j);
						lrAll[j] = bnkHits.getInt("LR", j);
						timeAll[j] = bnkHits.getDouble("time", j);
						docaAll[j] = bnkHits.getDouble("doca", j);
						trkDocaAll[j] = bnkHits.getDouble("trkDoca", j);
						xAll[j] = bnkHits.getDouble("X", j);
						zAll[j] = bnkHits.getDouble("Z", j);
						clustIDAll[j] = bnkHits.getInt("clusterID", j);
						timeResAll[j] = bnkHits.getDouble("timeResidual", j);
						docaErrAll[j] = bnkHits.getDouble("docaError", j);

						trkDoca = bnkHits.getDouble("trkDoca", j);

						if (debug > -1) {
							printIt = String.format("%d,%d,%d,%d,%d,%d, %.1f,%.3f,%.3f,%.2f,%.1f, %d,%.1f,%.3f",
									idAll[j], secAll[j], slAll[j], layAll[j], wireAll[j], lrAll[j], timeAll[j],
									docaAll[j], trkDocaAll[j], xAll[j], zAll[j], clustIDAll[j], timeResAll[j],
									docaErrAll[j]);
							System.out.println(printIt);
						}
						// For fast access (avoiding multiple loops), I am
						// indexing quantities with hitID (==id here)
						gLayer[id] = layAll[j];
						gWire[id] = wireAll[j];
						gTime[id] = timeAll[j];
						gDoca[id] = docaAll[j];
						gTrkDoca[id] = trkDocaAll[j];
						gX[id] = xAll[j];
						gZ[id] = zAll[j];
						gTimeRes[id] = timeResAll[j];
						if (debug > -1)
							System.out.println("trkDoca =" + trkDoca + "  trkDocaAll[j] =" + trkDocaAll[j]
									+ ",  gTrkDoca[id] = " + gTrkDoca[id]);

						docaBin = -1;
						docaBin = (int) ((trkDocaAll[j] - (-0.8)) / 0.2);// bin-range
																			// and
																			// width
																			// (-0.8,0.8),
																			// 0.2
						superlayer = slAll[j];
						layer = layAll[j];
						if (sector == 1 && (docaBin > -1 && docaBin < 8)) {
							hArrWire[superlayer - 1][layer - 1][docaBin].fill(wire);
						}
					}
				}

				if (debug > -1)
					System.out.println("============ Entries from TBSegments Bank:============ ");
				if (debug > -1)
					System.out.println("SegmID sec SL Hits(1,2,3,4,5,6,7,8,9,10,11,12) ClustID size mySize");
				if (tbSegs == true) {
					for (int k = 0; k < bnkSegs.rows(); k++) {
						nHitsInSeg = 0;
						ID = bnkSegs.getInt("ID", k);
						sector = bnkSegTrks.getInt("sector", k);
						superlayer = bnkSegs.getInt("superlayer", k);
						clusterID = bnkSegs.getInt("Cluster_ID", k);
						hitID[0] = bnkSegs.getInt("Hit1_ID", k);
						hitID[1] = bnkSegs.getInt("Hit2_ID", k);
						hitID[2] = bnkSegs.getInt("Hit3_ID", k);
						hitID[3] = bnkSegs.getInt("Hit4_ID", k);
						hitID[4] = bnkSegs.getInt("Hit5_ID", k);
						hitID[5] = bnkSegs.getInt("Hit6_ID", k);
						hitID[6] = bnkSegs.getInt("Hit7_ID", k);
						hitID[7] = bnkSegs.getInt("Hit8_ID", k);
						hitID[8] = bnkSegs.getInt("Hit9_ID", k);
						hitID[9] = bnkSegs.getInt("Hit10_ID", k);
						hitID[10] = bnkSegs.getInt("Hit11_ID", k);
						hitID[11] = bnkSegs.getInt("Hit12_ID", k);
						avgWire = (double) bnkSegs.getDouble("avgWire", k);
						fitChisqProb = (double) bnkSegs.getDouble("fitChisqProb", k);
						fitSlope = bnkSegs.getDouble("fitSlope", k);
						for (int h = 0; h < 12; h++) {
							if (hitID[h] > -1)
								nHitsInSeg++;
						}
						size = bnkSegTrks.getInt("size", k); // Should be equal
																// to nHitsInSeg
																// but for now
																// (6/10/16) it
																// is not.

						thDeg = rad2deg * Math.atan2(fitSlope, 1.0);
						thDeg2 = rad2deg * Math.atan(fitSlope);
						thBn = -1;
						thBnVz = -1;
						for (int th = 0; th < nTh; th++) {
							if (thDeg > thBins[th] && thDeg <= thBins[th + 1])
								thBn = th;
						}
						for (int th = 0; th < nThBinsVz; th++) {
							if (thDeg > thEdgeVzL[th] && thDeg <= thEdgeVzH[th])
								thBnVz = th;
						} // 8/31/16
							// following g-variables (global) to be accessed
							// from
							// other banks based on segmentID value.
						if (debug > -1)
							System.out.println("clusterID=" + clusterID);
						gSegmClustID[ID] = clusterID;
						gSegmAvgWire[ID] = avgWire;
						gFitChisqProb[ID] = fitChisqProb;
						gSegmTh[ID] = thDeg;
						gSegmThBin[ID] = thBn;

						h1ThSL[superlayer - 1].fill(thDeg);

						/*
						 * if(debug>-1) {
						 * System.out.println(ID+","+sector+","+superlayer+",");
						 * for(int h=0;h<12;h++)
						 * System.out.println(hitID[h]+",");
						 * System.out.println(clusterID); }
						 */

						if (debug > -1) {
							printIt = String.format("%d,%d,%d,%d,%d,  %d,%d,%d,%d,%d,  %d,%d,%d,%d,%d  ,%d,%d,%d", ID,
									sector, superlayer, hitID[0], hitID[1], hitID[2], hitID[3], hitID[4], hitID[5],
									hitID[6], hitID[7], hitID[8], hitID[9], hitID[10], hitID[11], clusterID, size,
									nHitsInSeg);
							System.out.println(printIt);
						}
						if (debug > -1)
							System.out.println(
									"hitID time thDeg_atan2 thDeg_atan trkDoca (associated hits info from TBhits bank)");
						double thTmp1 = thDeg - 0.0, thTmp2 = thDeg - 30.0;
						docaMax = 2.0 * wpdist[superlayer - 1];
						docaNorm = trkDoca / docaMax;
						for (int h = 0; h < 12; h++) {
							if (nHitsInSeg > 5)// Saving only those with more
												// than 5 hits
							{
								if (debug > -1 && hitID[h] > -1) {
									printIt = String.format("%02d %.1f %.1f %.1f %.1f", hitID[h], gTime[hitID[h]],
											thDeg, thDeg2, gTrkDoca[hitID[h]]);
									System.out.println(printIt);
								}

								if (hitID[h] > -1 && thBn > -1 && thBn < nTh)
									h1timeSlTh[superlayer - 1][thBn].fill(gTime[hitID[h]]);
								if (abs(thTmp1) < 1.0 && hitID[h] > -1)
									h2timeVtrkDoca[superlayer - 1][0].fill(abs(gTrkDoca[hitID[h]]), gTime[hitID[h]]);
								if (abs(thTmp2) < 1.0 && hitID[h] > -1)
									h2timeVtrkDoca[superlayer - 1][1].fill(abs(gTrkDoca[hitID[h]]), gTime[hitID[h]]);

								if (hitID[h] > -1 && thBnVz > -1 && thBnVz < nThBinsVz) {
									docaNorm = gTrkDoca[hitID[h]] / docaMax;
									deltaVal = abs(prevFitVal - gTime[hitID[h]]); // measurement
																					// value
									// if(deltaVal<50.0){ //11/10/16
									// h2timeVtrkDocaVZ[superlayer-1][thBnVz].fill(abs(gTrkDoca[hitID[h]]),gTime[hitID[h]]);
									h2timeVtrkDocaVZ[superlayer - 1][thBnVz].fill(abs(docaNorm), gTime[hitID[h]]);
									// } //11/10/16
								}
								// double thEdgeVzH[] = { 2.0, 12.0, 22.0, 32.0,
								// 42.0, 52.0};
								// H2F[][] h2timeVtrkDocaVZ = new
								// H2F[nSL][nThBinsVz]; //2 for 2 theta bins 0,
								// 30

								if (debug == -3 && hitID[h] > -1 && gLayer[hitID[h]] > -1)// Printing
																							// time
																							// &
																							// trkDoca
																							// info
																							// into
																							// a
																							// txt
																							// o/p
																							// file
																							// for
																							// time-to-distance
																							// calibration
								{
									// printIt = String.format("%d %d %d %d %.1f
									// %.1f
									// %.4f\n",ID,superlayer,gLayer[hitID[h]],
									// printIt = String.format("%d %d %d %d %d
									// %.1f %.1f %.4f %.1f
									// %.4f\n",counter,ID,superlayer,gLayer[hitID[h]],//Disabled
									// on 6/23/16
									printIt = String.format("%d %d %d %d %d %d %d %.1f %.1f %.4f %.1f %.4f %.4f %.4f\n",
											counter, ID, superlayer, gLayer[hitID[h]], nTBHits, nTBCrosses, hitID[h],
											thDeg, avgWire, fitChisqProb, gTime[hitID[h]], gTrkDoca[hitID[h]],
											gTimeRes[hitID[h]], gDoca[hitID[h]] // 7/19/16//gDoca
																				// added
																				// on
																				// 8/03/16
									);
									// file<<printIt;//<<"\n";
									appendIt(txtOp, printIt);// appendIt(txtOp,"Haha
																// ha ...2 \n");
																// //worked
									// writer<<printIt;//<<"\n";
								}
							}
						}

					}
				}

				if (debug > -1)
					System.out.println("============ Entries from TBSegmentTrajectory Bank:============ ");
				if (debug > -1)
					System.out.println("Entry#,Sec,SL,L,segmentID,matchedHitID trkDoca");
				if (tbSegTrks == true) {
					// EvioDataBank bnkSegTrks =
					// (EvioDataBank)event.getBank("TimeBasedTrkg::TBSegmentTrajectory");
					for (int i = 0; i < bnkSegTrks.rows(); i++) {
						// First getting all the values of each variables of the
						// current bank
						sector = bnkSegTrks.getInt("sector", i);
						superlayer = bnkSegTrks.getInt("superlayer", i);
						layer = bnkSegTrks.getInt("layer", i);
						segmentID = bnkSegTrks.getInt("segmentID", i);
						matchedHitID = bnkSegTrks.getInt("matchedHitID", i);
						trkDoca = bnkSegTrks.getDouble("trkDoca", i);
						SL = superlayer;
						dMax = wpdist[SL - 1];
						NtrkDoca = trkDoca / dMax; // 6/12/16

						// h1trkDoca4Dar= new H1F[nSL][nLayer][nTh][3];
						// Histos integrated over layer and theta
						h1trkDoca2Dar[SL - 1][0].fill(trkDoca);
						h1NtrkDoca2Dar[SL - 1][0].fill(NtrkDoca);
						h1NtrkDocaP2Dar[SL - 1][0].fill(abs(NtrkDoca));
						if (matchedHitID == -1) {
							h1trkDoca2Dar[SL - 1][1].fill(trkDoca);
							h1NtrkDoca2Dar[SL - 1][1].fill(NtrkDoca);
							h1NtrkDocaP2Dar[SL - 1][1].fill(abs(NtrkDoca));
						} // Used only for drawing
						if (matchedHitID == -1) {
							h1trkDoca2Dar[SL - 1][2].fill(trkDoca);
							h1NtrkDoca2Dar[SL - 1][2].fill(NtrkDoca);
							h1NtrkDocaP2Dar[SL - 1][2].fill(abs(NtrkDoca));
						} // Will be divided by tot.
							// Histos divided further into layer bins
						h1trkDoca3Dar[SL - 1][layer - 1][0].fill(trkDoca);
						h1NtrkDoca3Dar[SL - 1][layer - 1][0].fill(NtrkDoca);
						h1NtrkDocaP3Dar[SL - 1][layer - 1][0].fill(abs(NtrkDoca));
						if (matchedHitID == -1) {
							h1trkDoca3Dar[SL - 1][layer - 1][1].fill(trkDoca);
							h1NtrkDoca3Dar[SL - 1][layer - 1][1].fill(NtrkDoca);
							h1NtrkDocaP3Dar[SL - 1][layer - 1][1].fill(abs(NtrkDoca));
						} // Used only for drawing
						if (matchedHitID == -1) {
							h1trkDoca3Dar[SL - 1][layer - 1][2].fill(trkDoca);
							h1NtrkDoca3Dar[SL - 1][layer - 1][2].fill(NtrkDoca);
							h1NtrkDocaP3Dar[SL - 1][layer - 1][2].fill(abs(NtrkDoca));
						} // Will be divided by tot.
							// Histos divided further into theta bins
						if (segmentID > -1 && gSegmThBin[segmentID] > -1) {
							h1trkDoca4Dar[SL - 1][layer - 1][gSegmThBin[segmentID]][0].fill(trkDoca);
							if (matchedHitID == -1)
								h1trkDoca4Dar[SL - 1][layer - 1][gSegmThBin[segmentID]][1].fill(trkDoca);// Used
																											// only
																											// for
																											// drawing
							if (matchedHitID == -1)
								h1trkDoca4Dar[SL - 1][layer - 1][gSegmThBin[segmentID]][2].fill(trkDoca);// Will
																											// be
																											// divided
																											// by
																											// tot.

							// H1F[][][][] h1wire4Dar= new
							// H1F[nSL][nLayer][nTh][2], h1avgWire4Dar,
							// h1fitChisqProbSeg4Dar;
							if (matchedHitID > -1)
								h1wire4Dar[SL - 1][layer - 1][gSegmThBin[segmentID]][0].fill(gWire[matchedHitID]);// this
																													// wont
																													// have
																													// [][][][1]/[2]
																													// either
							h1avgWire4Dar[SL - 1][layer - 1][gSegmThBin[segmentID]][0].fill(gSegmAvgWire[segmentID]);
							h1fitChisqProbSeg4Dar[SL - 1][layer - 1][gSegmThBin[segmentID]][0]
									.fill(gFitChisqProb[segmentID]);
							if (matchedHitID == -1) {
								// h1wire4Dar[SL-1][layer-1][gSegmThBin[segmentID]][1].fill(gWire[matchedHitID]);//wont
								// work - no valid hitID
								h1avgWire4Dar[SL - 1][layer - 1][gSegmThBin[segmentID]][1]
										.fill(gSegmAvgWire[segmentID]);
								h1fitChisqProbSeg4Dar[SL - 1][layer - 1][gSegmThBin[segmentID]][1]
										.fill(gFitChisqProb[segmentID]);
							}
						}
						/*
						 * Now, looking at more associated info stored in a
						 * particular entry of another bank based on the value
						 * of segmentID. Unlike the hits related banks, these
						 * banks will have less # of entries. For example there
						 * could be just one or two clusters, segments, or
						 * tracks.
						 */

						if (debug == -2 && gSegmThBin[segmentID] == 4) { // To
																			// identify
																			// and
																			// look
																			// in
																			// CED
																			// some
																			// events
																			// that
																			// give
																			// rise
																			// to
																			// spikes
							boolean atSpike = false;
							// Spike positions at odd layers: 0.3697 in SL=1,
							// 0.3944 in SL=2
							// Spike positions at even layers: -02993 in SL=1,
							// -0.3057 in SL=2
							if (SL == 1 && (layer % 2 == 1) && (trkDoca > 0.36 && trkDoca < 0.37))
								atSpike = true; // For odd layers 1, 3, 5
							if (SL == 2 && (layer % 2 == 1) && (trkDoca > 0.39 && trkDoca < 0.40))
								atSpike = true;
							if (SL == 1 && (layer % 2 == 0) && (trkDoca > -0.30 && trkDoca < -0.29))
								atSpike = true; // For even layers 2,4,6
							if (SL == 2 && (layer % 2 == 0) && (trkDoca > -0.31 && trkDoca < -0.299))
								atSpike = true;
							if (atSpike == true) {
								printIt = String.format("Spikes: %d,%d,%d,%d,%d,%d,  %.3f", counter, sector, superlayer,
										layer, segmentID, matchedHitID, trkDoca);
								System.out.println(printIt);
							}
						}

						// if(debug>-1)
						// System.out.println(i+","+sector+","+superlayer+","+layer+","+segmentID+","+matchedHitID);
						if (debug > -1) {
							printIt = String.format("%d,%d,%d,%d,%d,%d,  %.3f", i, sector, superlayer, layer, segmentID,
									matchedHitID, trkDoca);
							System.out.println(printIt);
						}

						// docaMax = 2.0*wpdist[superlayer-1]; docaNorm =
						// trkDoca/docaMax;
					}
				}
			} else
				break;
			counter++;
		}
		/////////////// End of event loop within processData()
		/////////////// ///////////////////////////////

		// reader.close();
		String imgNm;
		// TCanvas c0 = new TCanvas("c0","trkDoca in Different
		// Layers",4*400,3*400,4,3);
		// TCanvas c0 = new TCanvas("c0",4*400,3*400); c0.divide(4,3);
		EmbeddedCanvas c0 = new EmbeddedCanvas();
		c0.setSize(4 * 400, 3 * 400);
		c0.divide(4, 3);
		GraphErrors[][] profileX = new GraphErrors[nSL][2]; // 2 for 2 theta
															// bins 0, 30
															// //h2.getProfileX();
		GraphErrors[][] profileY = new GraphErrors[nSL][2]; // 2 for 2 theta
															// bins 0, 30
															// //h2.getProfileX();
		// GraphErrors profileY = h2.getProfileY();
		for (int i = 0; i < nSL; i++) {
			for (int j = 0; j < 2; j++) { // 2 thet bins +/-1 deg around 0 and
											// 30 deg
				profileX[i][j] = h2timeVtrkDoca[i][j].getProfileX();
				profileY[i][j] = h2timeVtrkDoca[i][j].getProfileY();
				c0.cd(i * 2 + j);
				c0.draw(h2timeVtrkDoca[i][j]); // c0.draw(profileX[i][j],"same");
				c0.cd(i * 2 + j + 4);
				c0.draw(profileX[i][j]); // c0.draw(h2timeVtrkDoca[i][j]);
				c0.cd(i * 2 + j + 8);
				c0.draw(profileY[i][j]); // c0.draw(h2timeVtrkDoca[i][j]);
			}
		}
		// c0.cd(0); c0.draw(h2timeVtrkDoca[1][0]); c0.cd(1);
		// c0.draw(h2timeVtrkDoca[1][1]);
		// c0.cd(2); c0.draw(h2timeVtrkDoca[0][0]); c0.cd(3);
		// c0.draw(h2timeVtrkDoca[0][1]);
		imgNm = String.format("%s/timeVsTrkDoca_and_Profiles.png", oDir, dataType);
		c0.save(imgNm);

		/*
		 * // Capture the screen shot of the area of the screen defined by the
		 * rectangle //BufferedImage bi=robot.createScreenCapture(new
		 * Rectangle(100,100)); BufferedImage bi=robot.createScreenCapture(c1);
		 * //ImageIO.write(bi, "jpg", new File("C:/imageTest.jpg"));
		 * ImageIO.write(bi, "gif", new File(imgNm));
		 */

		// TCanvas c01 = new TCanvas("c01","time vs trkDoca (&
		// profile)",3*400,2*400,1,2);
		EmbeddedCanvas c01 = new EmbeddedCanvas();
		c01.setSize(3 * 400, 2 * 400);
		c01.divide(1, 2);
		c01.cd(0);
		c01.draw(h2timeVtrkDoca[0][0]);
		c01.cd(1);
		c01.draw(profileX[0][0]);
		imgNm = String.format("%s/timeVsTrkDoca_and_Profiles2.png", oDir, dataType);
		c01.save(imgNm);

		// TCanvas c03 = new TCanvas("c03","trkDoca/docaMax in Different #theta
		// bins",4*400,nThBinsVz*400,4,nThBinsVz);
		EmbeddedCanvas c03 = new EmbeddedCanvas();
		c03.setSize(4 * 400, nThBinsVz * 400);
		c03.divide(4, nThBinsVz);

		GraphErrors[][] profileXvz = new GraphErrors[nSL][nThBinsVz];
		for (int i = 0; i < nSL; i++) {
			for (int j = 0; j < nThBinsVz; j++) {
				profileXvz[i][j] = h2timeVtrkDocaVZ[i][j].getProfileX();
				// profileXvz[i][j] = h2timeVtrkDocaVZ[i][j].getProfileY();
			}
		}

		// Now start minimization
		KrishnaFcn theFCN = new KrishnaFcn(nSupLayers, nThBinsVz, profileXvz);
		MnUserParameters upar = new MnUserParameters();
		// parNames[] = {v0,deltamn,tmax1,tmax2,distbeta};
		// double parSteps[] = {0.00001 , 0.01 , 0.001, 0.01, 0.0001};
		double parSteps[] = { 0.00001, 0.001, 0.01, 0.01, 0.0001 };
		// double pLow[] = {prevFitPars[0]*0.6, prevFitPars[1]*0.6,
		// prevFitPars[2]*0.5, prevFitPars[3]*0.6, prevFitPars[4]*0.0};
		// double pHigh[] = {prevFitPars[0]*1.3, prevFitPars[1]*1.3,
		// prevFitPars[2]*1.5, prevFitPars[3]*1.3, prevFitPars[4]*1.5};
		double pLow[] = { prevFitPars[0] * 0.4, prevFitPars[1] * 0.0, prevFitPars[2] * 0.4, prevFitPars[3] * 0.4,
				prevFitPars[4] * 0.0 };
		double pHigh[] = { prevFitPars[0] * 1.6, prevFitPars[1] * 5.0, prevFitPars[2] * 1.6, prevFitPars[3] * 1.6,
				prevFitPars[4] * 1.6 };

		// upar.add("p0", 1.0, 0.1); upar.add("p1", 1.5, 0.1);
		for (int p = 0; p < nFreePars; p++) {
			/*
			 * upar.add(parName[p],prevFitPars[p],parSteps[p]); //Works
			 * //http://java.freehep.org/freehep-jminuit/apidocs/index-all.html
			 * // setLimits(int i, double low, double up)
			 * //http://java.freehep.org/freehep-jminuit/apidocs/org/freehep/
			 * math/minuit/package-summary.html
			 * upar.setLimits(p,pLow[p],pHigh[p]); //Works
			 */
			// Instead of above add() and setLimits(), I can use the following
			// add() to do the job for both
			upar.add(parName[p], prevFitPars[p], parSteps[p], pLow[p], pHigh[p]);
		}

		// =============== 10/4/16 (Fixing tmax parameters to 150 and 160 based
		// on my previous empirical estimates)
		// java.freehep.org/freehep-jminuit/apidocs/org/freehep/math/minuit/MnUserParameters.html
		upar.setValue(2, 155.0);
		upar.setValue(3, 165.0);
		upar.fix(2);
		upar.fix(3);
		// ===============

		System.out.println("Initial parameters: " + upar);

		System.out.println("start migrad");
		MnMigrad migrad = new MnMigrad(theFCN, upar);
		FunctionMinimum min = migrad.minimize();

		if (!min.isValid()) {
			// try with higher strategy
			System.out.println("FM is invalid, try with strategy = 2.");
			MnMigrad migrad2 = new MnMigrad(theFCN, min.userState(), new MnStrategy(2));
			min = migrad2.minimize();
		}

		// System.out.println("exit..."); System.exit(0); //kp
		System.out.println("kp: ===================================== 0");
		System.out.println("minimum: " + min);
		System.out.println("kp: ===================================== 1");

		MnUserParameters userpar = min.userParameters();

		// System.out.println("par0 = " + userpar.value("p0") + " +/-
		// " +
		// userpar.error("p0"));
		// System.out.println("par1 = " + userpar.value("p1") + " +/-
		// " +
		// userpar.error("p1"));
		for (int p = 0; p < nFreePars; p++)
			System.out.println(parName[p] + " = " + userpar.value(parName[p]) + " +/- " + userpar.error(parName[p]));
		double[] fPars = new double[nFreePars], fErrs = new double[nFreePars];

		for (int p = 0; p < nFreePars; p++) {
			fPars[p] = userpar.value(parName[p]);
			fErrs[p] = userpar.error(parName[p]);
		}

		System.out.println("\nJust for testing arrays filled with the fit-pars: ");
		for (int p = 0; p < nFreePars; p++)
			System.out.println(parName[p] + ": " + fPars[p] + " " + fErrs[p]);
		System.out.println("\n kp: ===================================== 2");

		System.out.println(
				"kp:========================== Just checking if the limits are coming out as the final values.");
		System.out.println("parID     parLow      parHigh");
		for (int p = 0; p < nFreePars; p++) {
			System.out.println(p + "  " + pLow[p] + "  " + pHigh[p]);
		}
		System.out.println(
				"kp:========================== Just checking if the limits are coming out as the final values.");

		for (int j = 0; j < nThBinsVz; j++) { // Row #
			c03.cd(j * 4 + 0);
			c03.draw(h2timeVtrkDocaVZ[0][j]); // c0.draw(profileX[i][j],"same");
			c03.cd(j * 4 + 1);
			c03.draw(h2timeVtrkDocaVZ[1][j]); // c0.draw(profileX[i][j],"same");
			c03.cd(j * 4 + 2);
			c03.draw(profileXvz[0][j]);
			c03.cd(j * 4 + 3);
			c03.draw(profileXvz[1][j]);
		}
		imgNm = String.format("%s/timeVsTrkDoca_and_ProfilesVZ.png", oDir, dataType);
		c03.save(imgNm);

		for (int i = 0; i < 5; i++)
			pars4FitLine[i] = fPars[i];
		pars4FitLine[5] = 1.0;
		pars4FitLine[6] = 0.0;
		pars4FitLine[7] = 0.3861;

		System.out.println("debug0 ..");
		calibFnToDraw_withGROOT[][] myFitLinesGroot = new calibFnToDraw_withGROOT[2][nThBinsVz];
		System.out.println("debug1 ..");
		for (int i = 0; i < nSL; i++) {
			for (int j = 0; j < nThBinsVz; j++) {
				hNm = String.format("myFitLinesS%dTh%d", i + 1, j);
				System.out.println("debug10 ..");
				myFitLinesGroot[i][j] = new calibFnToDraw_withGROOT(hNm, 0.0, 1.0);
				System.out.println("debug11 ..");
				myFitLinesGroot[i][j].setLineColor(3);
				myFitLinesGroot[i][j].setLineWidth(3);
				myFitLinesGroot[i][j].setLineStyle(4); // System.out.println("i="+i+",
														// j="+j);
				pars4FitLine[5] = 1.0 * (i + 1);
				pars4FitLine[6] = 0.5 * (thEdgeVzL[j] + thEdgeVzH[j]);
				pars4FitLine[7] = 2.0 * wpdist[i];
				myFitLinesGroot[i][j].setParameters(pars4FitLine);
				System.out.println("Groot f(0/0.5/1.0) = " + myFitLinesGroot[i][j].evaluate(0.0) + ", "
						+ myFitLinesGroot[i][j].evaluate(0.5) + ", " + myFitLinesGroot[i][j].evaluate(1.0));

			}
		}
		System.out.println("debug2 ..");

		// TCanvas c06 = new TCanvas("c06","time-to-distance
		// fits",1600,2400,4,6);
		// TCanvas c06 = new TCanvas("c06","time-to-distance
		// fits",1600,2400,4,6);
		EmbeddedCanvas c06 = new EmbeddedCanvas();
		c06.setSize(4 * 400, 6 * 400);
		c06.divide(4, 6);
		for (int j = 0; j < nThBinsVz; j++) {
			c06.cd(j * 4 + 0);
			c06.draw(h2timeVtrkDocaVZ[0][j]);
			c06.draw(myFitLinesGroot[0][j], "same");
			c06.cd(j * 4 + 1);
			c06.draw(h2timeVtrkDocaVZ[1][j]);
			c06.draw(myFitLinesGroot[1][j], "same");
			c06.cd(j * 4 + 2);
			c06.draw(profileXvz[0][j]);
			c06.draw(myFitLinesGroot[0][j], "same");
			c06.cd(j * 4 + 3);
			c06.draw(profileXvz[1][j]);
			c06.draw(myFitLinesGroot[1][j], "same");
		}
		imgNm = String.format("%s/myTestFitFunctionAllThBins_wdGroot.png", oDir, dataType);
		c06.save(imgNm);

		// 10/4/16: Trying to make plot of residuals for each superlayer
		H1F[] h1Residual = new H1F[nSL];
		for (int i = 0; i < nSL; i++) {
			hNm = String.format("ResidualS%d", i);
			h1Residual[i] = new H1F(hNm, 200, -1.0, 1.0);
		}
		// Now filling the histograms using data stored in earlier filled 2D
		// histos
		for (int i = 0; i < nSL; i++) {
			for (int j = 0; j < nThBinsVz; j++) {
			}
		}
	}// End of ProcessData()

	// Got " error: unreported exception IOException; must be caught or declared
	// to be thrown" when copiled without 'throws IOException' below
	public static void appendIt(String fileName, String Text) throws IOException {
		Writer output = new BufferedWriter(new FileWriter(fileName, true));
		output.append(Text);
		output.close();
	}

	//
	// Will have profile histograms of time-vs-doca distributions in 6 different
	// theta-bins and for 2 diff SL
	// May be we can make the constructor KrishnaFcn(..) that takes in two more
	// arrays
	// one carrying the SL values, and another with the theta-bin value for each
	// event so that these two
	// additional info can be used later to properly use the equations/functions
	// to evaluate 'nexp'
	// Following was used for a str. line fit.
	//
	static class KrishnaFcn implements FCNBase {
		// KrishnaFcn(double[] xVals, double[] meas) //kp: meas can also be
		// named yVals
		KrishnaFcn(int slNum, int nThBins, GraphErrors[][] profile) // kp: meas
																	// can also
																	// be named
																	// yVals
		{
			profileX = profile;
			nSL = slNum;
			nThBinsVZ = nThBins;
		}

		public double errorDef() {
			return 1;
		}// kp: Looks like this fn must be defined, even if not used here.

		public double valueOf(double[] par) {
			double c = par[0], m = par[1]; // straight line equation: y = m*x +
											// c
			double delta = 0., chisq = 0., fval = 0., thetaDeg = 0., docaNorm = 0., measTime = 0., measTimeErr = 0.,
					calcTime = 0.;
			for (int sl = 0; sl < nSL; sl++) {
				// for(int th=0;th<nThBinsVZ;th++)
				for (int th = 1; th < 3; th++) // Using only some theta bins
												// between 0 and 30 degress
				{
					// thetaDeg = thEdgeVzL[th] + thEdgeVzH[th];
					thetaDeg = 0.5 * (thEdgeVzL[th] + thEdgeVzH[th]);// No 0.5
																		// factor
																		// used
																		// before
																		// 9/20/16
					// for( int i = 0; i < theMeasurements.length; i++)
					// for(int i=0; i<profileX[sl][th].getDataSize(); i++)
					for (int i = 0; i < profileX[sl][th].getDataSize(0); i++) {
						// System.out.println("i X(i) ErrorX(i) Y(i) ErrorY(i):
						// " + i + " "
						// + profileX[sl][th].getDataX(i) + " " +
						// profileX[sl][th].getErrorX(i)
						// + " " + profileX[sl][th].getDataY(i) + " " +
						// profileX[sl][th].getErrorY(i));

						docaNorm = profileX[sl][th].getDataX(i);
						measTime = profileX[sl][th].getDataY(i);
						// measTimeErr = profileX[sl][th].getErrorY(i);
						measTimeErr = profileX[sl][th].getDataEY(i);

						calcTime = calcTimeFunc(-1, sl + 1, thetaDeg, docaNorm, par);

						// 9/27/16: without docaNorm<0.9, the minimization was
						// very unstable. For example,
						// tmax for SL=2 (i.e. tmax2) came out around 150 when
						// the # of events used was N=20000 or 200000
						// where as it came out around 88 ns when N was
						// somewhere in between such as 80000, 100000 etc.
						// My guess was some of the bins with very low statistic
						// had unrealistic errors bars and biased
						// the minimization. When I used "delta = (measTime -
						// calcTime);", the tmax2 result was more
						// realistic (i.e., closer to 150 ns) than 88 ns.
						// if(measTimeErr==measTimeErr && measTimeErr>0.0 )
						if (measTimeErr == measTimeErr && measTimeErr > 0.0 && docaNorm < 0.9) {
							delta = (measTime - calcTime) / measTimeErr; // error
																			// weighted
																			// deviation
							// delta = (measTime - calcTime);// /measTimeErr;
							// //error weighted deviation
							chisq += delta * delta;
							// System.out.println("chiSq, delta, tMes, tCalc,
							// tErr: " +
							// chisq + " " + delta + " " + measTime + " " +
							// calcTime + " " + measTimeErr + " par1/2/3: " +
							// par[1] + " " + par[2] + " " + par[3]);
						}
						// chisq += (measTime - calcTime)*(measTime -
						// calcTime);///ei;
					}
				}
			}
			System.out.println("chisq = " + chisq);
			return chisq;// fval;
		}

		public double calcTimeFunc(int debug, int SL, double thetaDeg, double docaByDocaMax, double[] par) // 9/4/16
		{
			// From one of M. Mestayer's email:
			// Double_t time = x/v0 + a0*pow(Xhat0, n) + b0*pow(Xhat0,m); //Here
			// X = x/(dMax*cos(30deg));
			// deltanm=2; m = n - deltanm; b = (tmax - dmax/v0)(1.0 - m/n); a =
			// -b* m/n; (see Mac's email)
			// tmax: Krishna's plots show tmax to be 165, 174 ns for superlayers
			// 1 and 2
			// For superlayers 3 and 4 let's use 300 and 320 (this is for zero
			// B-field)
			// and for superlayers 5 and 6 let's use 530 and 560 ns.
			// deltanm: let's start with a value of 2. double calcTime = 0;
			// //slope*xCoordinate + yIntercept;

			double dMax = 2 * wpdist[SL - 1], Dc = dMax * cos30;
			// double cos30 = Math.cos(30.0/rad2deg);//Now it's a global
			// constant to avoid repeated calc. (see above main())
			double X = docaByDocaMax, x = X * dMax, Xhat0 = X / cos30;
			double v0Par = par[0], deltanm = par[1], tMax = par[2];
			if (SL == 2)
				tMax = par[3];
			double distbeta = par[4]; // 8/3/16: initial value given by Mac is
										// 0.050 cm.

			// Assume a functional form (time =
			// x/v0+a*(x/dmax)**n+b*(x/dmax)**m) for theta = 30 deg.
			// First, calculate n
			double nPar = (1.0 + (deltanm - 1.0) * Math.pow(0.615, deltanm)) / (1.0 - Math.pow(0.615, deltanm));

			// now, calculate m
			double mPar = nPar + deltanm;// Actually it should be named deltamn
											// and should be + in between
											// //7/21/16
			// determine b from the constraint that the time = tmax at dist=dmax
			double b = (tMax - dMax / v0Par) / (1.0 - mPar / nPar);

			// determine a from the requirement that the derivative at
			// d=dmax equal the derivative at d=0
			double a = -b * mPar / nPar; // From one of the constraints
			double alpha = thetaDeg; // = 0.0; //Local angle in degrees.
			double cos30minusalpha = Math.cos((30. - alpha) / rad2deg); // =Math.cos(Math.toRadians(30.-alpha));
			double xhat = x / dMax, dmaxalpha = dMax * cos30minusalpha, xhatalpha = x / dmaxalpha;

			// now calculate the dist to time function for theta = 'alpha' deg.
			// Assume a functional form with the SAME POWERS N and M and
			// coefficient a but a new coefficient 'balpha' to replace b.
			// Calculate balpha from the constraint that the value
			// of the function at dmax*cos30minusalpha is equal to tmax

			// parameter balpha (function of the 30 degree paramters a,n,m)
			double balpha = (tMax - dmaxalpha / v0Par - a * Math.pow(cos30minusalpha, nPar))
					/ Math.pow(cos30minusalpha, mPar);

			// now calculate function
			double xhatPowN = Math.pow(xhat, nPar), xhatPowM = Math.pow(xhat, mPar);
			double term1 = x / v0Par, term2 = a * xhatPowN, term3 = balpha * xhatPowM;
			// double time = x/v0Par + a*pow(xhat, nPar) + balpha*pow(xhat,
			// mPar);
			double calcTime = term1 + term2 + term3;

			if (debug == 1)
				System.out.println("v0Par nPar  b  a  xhat xhatalpha  xhatPowN  xhatPowM  term1  term2  term3  time: "
						+ v0Par + " " + nPar + " " + b + " " + a + " " + xhat + " " + xhatalpha + " " + xhatPowN + " "
						+ xhatPowM + " " + term1 + " " + term2 + " " + term3 + " " + calcTime);

			// ===================== 8/3/16
			// double deltatime_beta=(sqrt(x**2+(distbeta*beta**2)**2)-x)/v0;
			// //where x is trkdoca
			double deltatime_beta = (Math.sqrt(x * x + Math.pow(distbeta * Math.pow(beta, 2), 2)) - x) / v0Par;
			calcTime = calcTime + deltatime_beta;
			// ===================== 8/3/16

			return calcTime;
		}

		private int nSL, nThBinsVZ;
		private GraphErrors[][] profileX;
	}

	// Following was used for a str. line fit.
	static class KrishnaQuickTest {
		KrishnaQuickTest(int slNum, int nThBins, GraphErrors[][] profile) // kp:
																			// [sl][thBin]
		{
			profileX = profile;
			nSL = slNum;
			nThBinsVZ = nThBins;
		}

		public double printSomeValues() {
			// double c = par[0], m = par[1]; //straight line equation: y = m*x
			// + c
			double chisq = 0., fval = 0.;
			for (int sl = 0; sl < nSL; sl++) {
				for (int th = 0; th < nThBinsVZ; th++) {

				}
			}
			// for(int i=0; i<profileX[0][0].getDataSize(); i++) //getDataSize()
			// valid only for coatjava2.4 plotting packag
			for (int i = 0; i < profileX[0][0].getDataSize(0); i++) {
				/*
				 * System.out.println("i X(i) ErrorX(i)  Y(i) ErrorY(i): " + i +
				 * " " + profileX[0][0].getDataX(i) + " " +
				 * profileX[0][0].getErrorX(i) + " " +
				 * profileX[0][0].getDataY(i) + " " +
				 * profileX[0][0].getErrorY(i));
				 */ // valid only for coatjava2.4 plotting packag
				System.out.println("i X(i) ErrorX(i)  Y(i) ErrorY(i): " + i + " " + profileX[0][0].getDataX(i) + " "
						+ profileX[0][0].getDataEX(i) + " " + profileX[0][0].getDataY(i) + " "
						+ profileX[0][0].getDataEY(i));
			}
			return chisq;// fval;
		}

		private int nSL, nThBinsVZ;
		private GraphErrors[][] profileX;
	}
	// }
	// public static void System.out.println(String str)
	// {System.out.println(str);}
	// public static void System.out.print(String str)
	// {System.out.print(str);}
}
