/*
             * To change this license header, choose License Headers in Project Properties.
             * To change this template file, choose Tools | Templates
             * and open the template in the editor.
 */
package org.jlab.dc_calibration;

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
	private String fileName;
	private int x = 0, y = 0;
	private boolean acceptorder = false;
	
	private OrderOfAction OAInstance;

	public ReadRecDataIn(OrderOfAction OA) {
		OAInstance = OA;
		
		String fDir = "/Volumes/Mac_Storage/Work_Codes/CLAS12/DC_Calibration/data/";
		String fName = "reconstructedDataR128T0corT2DfromCCDBvarFit08.1.evio";
		this.fileName = fDir + fName;
		System.out.println("In default constructor with fileName of " + fileName);
	}

	public ReadRecDataIn(String fileName) {
		this.fileName = fileName;
		System.out.println("In constructor with fileName of " + fileName);

	}

	public void actionPerformed(ActionEvent ev) {
		String myMessage = null;
		myMessage = String.format("Hello from Dialog inside TestEvent class!! x=%d, y=%d", x, y);

		JFrame frame = new JFrame("JOptionPane showMessageDialog example1");

		// show a joptionpane dialog using showMessageDialog
		// JOptionPane.showMessageDialog(frame, myMessage);
		
		System.out.println("In action Performed with fileName of " + fileName);

		OAInstance.buttonstatus(ev);
		acceptorder = OAInstance.isorderOk();
		
		if(acceptorder){
			JOptionPane.showMessageDialog(frame, "Click OK to start reading the reconstructed file ...");
		  processData();
		}else System.out.println("I am red and it is not my turn now ;( ");
	}

	public void processData() {
		
		System.out.println("I am green and now I should do something here...");
		
		/*
		System.err.println("this better be the file" + fileName);
		long printEvent;
		if (debug) {
			printEvent = 1;
		} else {
			printEvent = printEventNr;
		}

		int inFileNum = 1;
		EvioDataChain reader = new EvioDataChain();
		// for (String inputFile : inputFiles) {
		for (int fN = 0; fN < inFileNum; fN++) {
			// if (inputFile == null) { break; }
			reader.addFile(fileName);
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
		*/
	}

	// }
	public static void println(String str) {
		System.out.println(str);
	}

	public static void print(String str) {
		System.out.print(str);
	}
}
