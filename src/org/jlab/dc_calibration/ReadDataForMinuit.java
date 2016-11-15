/*  +__^_________,_________,_____,________^-.-------------------,
 *  | |||||||||   `--------'     |          |                   O
 *  `+-------------USMC----------^----------|___________________|
 *    `\_,---------,---------,--------------'
 *      / X MK X /'|       /'
 *     / X MK X /  `\    /'
 *    / X MK X /`-------'
 *   / X MK X /
 *  / X MK X /
 * (________(                @author m.c.kunkel
 *  `------'
*/
package org.jlab.dc_calibration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.base.DataBank;
import org.jlab.io.evio.EvioDataBank;
import org.jlab.io.evio.EvioDataChain;
import org.jlab.io.evio.EvioDataEvent;

public class ReadDataForMinuit {

	String file;
	EvioDataChain reader = null;
	EvioDataBank bnkHits = null;
	List<EvioDataBank> arBanks;
	static int debug = -1;

	public ReadDataForMinuit(String file) {
		this.file = file;
		this.reader = new EvioDataChain();
		createHists();
	}

	private final int nSL = 2;
	private final int nLayer = 6;
	private final int nHists = 8;
	private final int nTh = 9;
	private final int nSupLayers = 2;
	private final int nThBinsVz = 6; // [nThBinsVZ][2]
	private final double[] thEdgeVzL = { -2.0, 8.0, 18.0, 28.0, 38.0, 48.0 };
	private final double[] thEdgeVzH = { 2.0, 12.0, 22.0, 32.0, 42.0, 52.0 };

	public H1F[][][] hArrWire = new H1F[nSL][nLayer][nHists];
	H1F[] h1ThSL = new H1F[nSL];
	H1F[][] h1timeSlTh = new H1F[nSL][nTh];

	private void createHists() {
		hArrWire[0][0][0] = new H1F("wireDb0_0_0", 120, -1.0, 119.0);
		String hNm = "";
		String hTtl = "";
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
		///////
		int thBn = -1, thBnVz = -1;
		h1ThSL[0] = new H1F("thetaSL1", 120, -60.0, 60.0);
		h1ThSL[0].setTitle("#theta");
		h1ThSL[0].setLineColor(1);
		h1ThSL[1] = new H1F("thetaSL2", 120, -60.0, 60.0);
		h1ThSL[1].setTitle("#theta");
		h1ThSL[1].setLineColor(2);

		double[] thBins = { -60.0, -40.0, -20.0, -10.0, -1.0, 1.0, 10.0, 20.0, 40.0, 60.0 };
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
		H1F[][] h1trkDoca2Dar = new H1F[nSL][3], h1NtrkDoca2Dar = new H1F[nSL][3], h1NtrkDocaP2Dar = new H1F[nSL][3];// [3] for all good hits, only bad (matchedHitID
		                                                                                                             // == -1) and ratio

		H1F[][][] h1trkDoca3Dar = new H1F[nSL][nLayer][3], h1NtrkDoca3Dar = new H1F[nSL][nLayer][3],
		        h1NtrkDocaP3Dar = new H1F[nSL][nLayer][3]; // [3] for all good hits, only bad (matchedHitID== -1) and ratio

		H1F[][][][] h1trkDoca4Dar = new H1F[nSL][nLayer][nTh][3];
		H1F[][][][] h1wire4Dar = new H1F[nSL][nLayer][nTh][2], h1avgWire4Dar = new H1F[nSL][nLayer][nTh][2];// no ratio here

		H1F[][][][] h1fitChisqProbSeg4Dar = new H1F[nSL][nLayer][nTh][2];
		String[] hType = { "all hits", "matchedHitID==-1", "Ratio==Ineff." };// as String[];

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
							hTtl = String.format("all hits (SL=%d, Layer%d, th(%.1f,%.1f))", i + 1, j + 1, thBins[th], thBins[th + 1]);
						if (k == 1)
							hTtl = String.format("matchedHitID==-1 (SL=%d, Layer%d, th(%.1f,%.1f))", i + 1, j + 1, thBins[th],
							        thBins[th + 1]);
						if (k == 2)
							hTtl = String.format("Ineff. (SL=%d, Layer%d, th(%.1f,%.1f))", i + 1, j + 1, thBins[th], thBins[th + 1]);
						h1trkDoca4Dar[i][j][th][k].setTitle(hTtl);
						h1trkDoca4Dar[i][j][th][k].setLineColor(i + 1);
					}
					for (int k = 0; k < 2; k++) {
						hNm = String.format("wireS%dL%dTh%02dH%d", i + 1, j + 1, th, k);
						h1wire4Dar[i][j][th][k] = new H1F(hNm, 120, -1.0, 119.0);
						hTtl = String.format("wire # for %s (SL=%d, Lay%d, th(%.1f,%.1f))", hType[k], i + 1, j + 1, thBins[th],
						        thBins[th + 1]);
						h1wire4Dar[i][j][th][k].setTitle(hTtl);
						h1wire4Dar[i][j][th][k].setLineColor(i + 1);
						hNm = String.format("avgWireS%dL%dTh%02dH%d", i + 1, j + 1, th, k);
						h1avgWire4Dar[i][j][th][k] = new H1F(hNm, 120, -1.0, 119.0);
						hTtl = String.format("avgWire(SegBnk) for %s (SL=%d, Lay%d, th(%.1f,%.1f))", hType[k], i + 1, j + 1, thBins[th],
						        thBins[th + 1]);
						h1avgWire4Dar[i][j][th][k].setTitle(hTtl);
						h1avgWire4Dar[i][j][th][k].setLineColor(i + 1);
						hNm = String.format("fitChisqProbS%dL%dTh%02dH%d", i + 1, j + 1, th, k);
						h1fitChisqProbSeg4Dar[i][j][th][k] = new H1F(hNm, 90, -0.1, 0.1);
						hTtl = String.format("fitChisqProbSeg(SegBnk) for %s (SL=%d, Lay%d, th(%.1f,%.1f))", hType[k], i + 1, j + 1,
						        thBins[th], thBins[th + 1]);
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

		H2F[][] h2timeVtrkDocaVZ = new H2F[nSL][nThBinsVz]; // 2 for 2 theta bins 0, 30

		for (int i = 0; i < nSL; i++) {
			for (int j = 0; j < nThBinsVz; j++) { // nThBinsVZ theta bins +/-2
			                                      // deg around 0, 10, 20, 30,
			                                      // 40, and 50 degs
				hNm = String.format("timeVtrkDocaS%dTh%02d", i, j);
				h2timeVtrkDocaVZ[i][j] = new H2F(hNm, 200, 0.0, 1.0, 150, 0.0, 200.0);
				hTtl = String.format("time vs |trkDoca| (SL=%d, th(%2.1f,%2.1f))", i + 1, thEdgeVzL[j], thEdgeVzH[j]); // Worked
				h2timeVtrkDocaVZ[i][j].setTitle(hTtl); // h2timeVtrkDoca[i][j].setMarkerColor(2);
			}
		}

		///////
	}

	public void processData() {
		reader.addFile(this.file);
		reader.open();

		int counter = 0, NumEv2process = 20, nTBHits = 0;

		/////////////// Start of event loop within processData()
		EvioDataBank bnkClust = null, bnkSegs = null, bnkSegTrks = null, bnkCross = null;
		List<DataBank> arrEvioDataBank = new ArrayList<DataBank>();
		while (reader.hasEvent() && counter < 10) {
			counter++;
			System.out.println("here");

			EvioDataEvent event = reader.getNextEvent();
			boolean tbHits = false;
			if (event.hasBank("TimeBasedTrkg::TBHits")) { // bnkHits.show();//printout
				// the content
				// of the bank
				bnkHits = (EvioDataBank) event.getBank("TimeBasedTrkg::TBHits");
				tbHits = true;
				nTBHits = bnkHits.rows();
				arrEvioDataBank.add(event.getBank("TimeBasedTrkg::TBHits"));
				Map<Integer, Integer> testmap = new HashMap<Integer, Integer>();
				Map<Integer, BankStructure> bankmap = new HashMap<Integer, BankStructure>();

				// gLayer[id] = layAll[j];
				// gWire[id] = wireAll[j];
				// gTime[id] = timeAll[j];
				// gDoca[id] = docaAll[j];
				// gTrkDoca[id] = trkDocaAll[j];
				// gX[id] = xAll[j];
				// gZ[id] = zAll[j];
				// gTimeRes[id] = timeResAll[j];
				for (int j = 0; j < nTBHits; j++) {
					testmap.put(bnkHits.getInt("id", j), bnkHits.getInt("layer", j));
					bankmap.put(bnkHits.getInt("id", j), new BankStructure("layer", bnkHits.getInt("layer", j)));
				}
			}

			// if (tbHits == true) {
			// idAll = new int[nTBHits];
			// secAll = new int[nTBHits];
			// slAll = new int[nTBHits];
			// layAll = new int[nTBHits];
			// wireAll = new int[nTBHits];
			// lrAll = new int[nTBHits];
			//
			// timeAll = new double[nTBHits];
			// docaAll = new double[nTBHits];
			// trkDocaAll = new double[nTBHits];
			// xAll = new double[nTBHits];
			// zAll = new double[nTBHits];
			// clustIDAll = new int[nTBHits];
			// timeResAll = new double[nTBHits];
			// docaErrAll = new double[nTBHits];
			// for (int j = 0; j < nTBHits; j++) {
			// id = bnkHits.getInt("id", j);
			// idAll[j] = bnkHits.getInt("id", j);
			// secAll[j] = bnkHits.getInt("sector", j);
			// slAll[j] = bnkHits.getInt("superlayer", j);
			// layAll[j] = bnkHits.getInt("layer", j);
			// wireAll[j] = bnkHits.getInt("wire", j);
			// lrAll[j] = bnkHits.getInt("LR", j);
			// timeAll[j] = bnkHits.getDouble("time", j);
			// docaAll[j] = bnkHits.getDouble("doca", j);
			// trkDocaAll[j] = bnkHits.getDouble("trkDoca", j);
			// xAll[j] = bnkHits.getDouble("X", j);
			// zAll[j] = bnkHits.getDouble("Z", j);
			// clustIDAll[j] = bnkHits.getInt("clusterID", j);
			// timeResAll[j] = bnkHits.getDouble("timeResidual", j);
			// docaErrAll[j] = bnkHits.getDouble("docaError", j);
			//
			// trkDoca = bnkHits.getDouble("trkDoca", j);
			//
			// if (debug > -1) {
			// printIt = String.format("%d,%d,%d,%d,%d,%d,
			// %.1f,%.3f,%.3f,%.2f,%.1f, %d,%.1f,%.3f", idAll[j],
			// secAll[j], slAll[j], layAll[j], wireAll[j], lrAll[j], timeAll[j],
			// docaAll[j],
			// trkDocaAll[j], xAll[j], zAll[j], clustIDAll[j], timeResAll[j],
			// docaErrAll[j]);
			// System.out.println(printIt);
			// }
			// // For fast access (avoiding multiple loops), I am
			// // indexing quantities with hitID (==id here)
			// gLayer[id] = layAll[j];
			// gWire[id] = wireAll[j];
			// gTime[id] = timeAll[j];
			// gDoca[id] = docaAll[j];
			// gTrkDoca[id] = trkDocaAll[j];
			// gX[id] = xAll[j];
			// gZ[id] = zAll[j];
			// gTimeRes[id] = timeResAll[j];
			// if (debug > -1)
			// System.out.println("trkDoca =" + trkDoca + " trkDocaAll[j] =" +
			// trkDocaAll[j]
			// + ", gTrkDoca[id] = " + gTrkDoca[id]);
			//
			// docaBin = -1;
			// docaBin = (int) ((trkDocaAll[j] - (-0.8)) / 0.2);// bin-range
			// // and
			// // width
			// // (-0.8,0.8),
			// // 0.2
			// superlayer = slAll[j];
			// layer = layAll[j];
			// if (sector == 1 && (docaBin > -1 && docaBin < 8)) {
			// hArrWire[superlayer - 1][layer - 1][docaBin].fill(wire);
			// }
			// }
			// }
			//
			// }
			/////////////// End of event loop within processData()
		}
	}

	private class BankStructure {
		protected Number iValue;
		protected String name;

		public BankStructure(String name, Number iValue) {
			this.name = name;
			this.iValue = iValue;
		}

		public Number getiValue() {
			return iValue;
		}

		public void setiValue(Number iValue) {
			this.iValue = iValue;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	public static void main(String[] args) {
		String fileName = "/Volumes/Mac_Storage/Work_Codes/CLAS12/DC_Calibration/data/reconstructedDataR128T0corT2DfromCCDBvarFit08.1.evio";
		ReadDataForMinuit rd = new ReadDataForMinuit(fileName);

		rd.processData();

	}
}
