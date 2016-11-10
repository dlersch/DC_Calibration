/*
             * To change this license header, choose License Headers in Project Properties.
             * To change this template file, choose Tools | Templates
             * and open the template in the editor.
 */
package java.org.jlab.dc_calibration;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jlab.io.evio.EvioDataBank;
import org.jlab.io.evio.EvioDataChain;
import org.jlab.io.evio.EvioDataEvent;

/**
 *
 * @author KPAdhikari
 */
public class ReadRecDataIn implements ActionListener {

	int eventNr;
	static boolean debug = false; // for debugging
	protected static java.util.List<String> inputFiles;
	static long numEvents = 15000;// (long) 1e9; // events to process
	static long printEventNr = 20000; // display progress

	private int x = 0, y = 0;

	public ReadRecDataIn() {
		this.x = 3;
		this.y = 5;
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
		processData();
	}

	public void processData() {
		long printEvent;
		if (debug) {
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
		println("Opened the input data file (from ReadRecDataIn class) !");

		int counter = 0, NumEv2process = 20, nTBHits = 0;
		EvioDataBank bnkHits = null;

		// Now loop over all the events
		while (reader.hasEvent()) {
			EvioDataEvent event = reader.getNextEvent();
			boolean tbHits = false;
			if (counter < NumEv2process) {
				if (event.hasBank("TimeBasedTrkg::TBHits")) {
					bnkHits = (EvioDataBank) event.getBank("TimeBasedTrkg::TBHits");
					tbHits = true;
					nTBHits = bnkHits.rows();
					println("# of hist in this " + counter + "th event = " + nTBHits);
				}
			}
			counter++;
		}
	}

	// }
	public static void println(String str) {
		System.out.println(str);
	}

	public static void print(String str) {
		System.out.print(str);
	}
}
