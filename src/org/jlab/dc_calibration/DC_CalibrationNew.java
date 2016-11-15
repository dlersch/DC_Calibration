/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.dc_calibration;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class DC_CalibrationNew extends WindowAdapter implements WindowListener, ActionListener, Runnable {

	private JFrame frame;
	private JTextArea textArea;
	protected Thread reader;
	protected Thread reader2;
	private boolean quit;

	private final PipedInputStream pin = new PipedInputStream();
	private final PipedInputStream pin2 = new PipedInputStream();

	Thread errorThrower; // just for testing (Throws an Exception at this
	                     // Console
	Thread mythread; // kp

	public JLabel banner;
	JButton bFileChooser, buttonClear;
	JFileChooser fc;

	private String fileName;

	public DC_CalibrationNew() {
		// create all components and add them
		frame = new JFrame("Java Console");
		frame.setLayout(new BorderLayout());// kp

		// kp: Defining the size of the window based on screenSize
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = new Dimension((int) (screenSize.width / 2), (int) (screenSize.height / 2));
		int x = (int) (frameSize.width / 2); // kp: quarter of screen width
		int y = (int) (frameSize.height / 2);// kp: quarter of screen height
		frame.setBounds(x, y, frameSize.width, frameSize.height);
		// frame.setBounds(0,0,frameSize.width,frameSize.height);//kp 0,0 is the
		// coordinate of top-left corner

		// Set up banner to use as custom preview panel
		banner = new JLabel("Welcome to DC calibration for CLAS12 ... !", JLabel.CENTER);
		banner.setForeground(Color.yellow);
		// banner.setBackground(Color.blue);
		banner.setBackground(Color.gray);
		banner.setOpaque(true);
		banner.setFont(new Font("SansSerif", Font.BOLD, 20));
		// kp:
		// http://stackoverflow.com/questions/1783793/java-difference-between-the-setpreferredsize-and-setsize-methods-in-compone
		// banner.setPreferredSize(new Dimension(600, 65));
		banner.setPreferredSize(new Dimension(1000, 30));

		JPanel bannerPanel = new JPanel(new BorderLayout());
		bannerPanel.add(banner, BorderLayout.CENTER);
		bannerPanel.setBorder(BorderFactory.createTitledBorder("Welcome Banner"));

		// Create a file Chooser
		fc = new JFileChooser();

		// kp: Set up Panel to hold several buttons.
		JPanel panelForButtons = new JPanel(new BorderLayout());
		// panelForButtons.add(banner, BorderLayout.CENTER);
		// bFileChooser = new JButton("Choose File");
		bFileChooser = new JButton("Choose File", createImageIcon("/images/Open16.gif"));
		bFileChooser.addActionListener(this);

		JButton bT0Cor = new JButton("Estimate & Apply T0 Correction");
		JButton bDecoder = new JButton("Run Decoder");
		JButton bReconst = new JButton("Run Reconstruction");
		JButton bT2Dfitter = new JButton("Run DOCA-to-Time Fitter");
		JButton ccdbWriter = new JButton("Send Results to CCDB");

		JPanel p3buttons = new JPanel(new BorderLayout());
		p3buttons.add(bDecoder, BorderLayout.LINE_START);
		p3buttons.add(bReconst, BorderLayout.LINE_END);

		// As I cannot put two components (bReconst and bT2Dfitter) in the
		// CENTER area of the BorderLayout, I need to first package the two
		// into a new JPanel as follows
		JPanel p2buttons = new JPanel(new BorderLayout());
		p2buttons.add(bT0Cor, BorderLayout.LINE_START); // WEST); //WEST also
		                                                // works
		p2buttons.add(p3buttons, BorderLayout.CENTER);
		p2buttons.add(bT2Dfitter, BorderLayout.LINE_END); // EAST also works

		panelForButtons.add(bFileChooser, BorderLayout.LINE_START);
		panelForButtons.add(p2buttons, BorderLayout.CENTER);
		panelForButtons.add(ccdbWriter, BorderLayout.LINE_END);

		panelForButtons.setBorder(BorderFactory.createEtchedBorder());

		JPanel panWelcomeAndButtons = new JPanel(new BorderLayout());
		panWelcomeAndButtons.add(bannerPanel, BorderLayout.NORTH);
		panWelcomeAndButtons.add(panelForButtons, BorderLayout.SOUTH);

		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setPreferredSize(new Dimension(300, 380));
		buttonClear = new JButton("clear");
		buttonClear.addActionListener(this); // done below

		JPanel panelImg = new JPanel(new BorderLayout());

		ImageIcon imageIcon = new ImageIcon(this.getClass().getResource("/images/blue.gif"));
		// ImageIcon imageIcon = new
		// ImageIcon(this.getClass().getResource("images/timeVsTrkDoca_and_Profiles.png"));
		JLabel imgLabel = new JLabel(imageIcon);
		panelImg.add(imgLabel, BorderLayout.CENTER);

		TestEvent e1 = new TestEvent();
		bT0Cor.addActionListener(e1);
		ReadRecDataIn e2 = new ReadRecDataIn(fileName);
		bDecoder.addActionListener(e2);
		ReadRecDataForMinuit e3 = new ReadRecDataForMinuit();
		bT2Dfitter.addActionListener(e3);

		// panelImg.add(imageIcon, BorderLayout.CENTER);
		// JPanel panelImg = new JPanel(imageIcon,new BorderLayout());
		JPanel panTextAndImg = new JPanel(new BorderLayout());
		panTextAndImg.add(new JScrollPane(textArea), BorderLayout.EAST);
		// panTextAndImg.add(textArea, BorderLayout.EAST);
		// panTextAndImg.add(panelImg, BorderLayout.WEST);
		panTextAndImg.add(new JScrollPane(panelImg), BorderLayout.WEST);

		// I think we need getContentPane() with JFrame, but not with JPanel
		// object
		// I guess, getContentPane() is equivalent to getting the JPanel object
		// contained in the frame.
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(panWelcomeAndButtons, BorderLayout.NORTH); // kp
		// frame.getContentPane().add(new
		// JScrollPane(textArea),BorderLayout.CENTER);
		frame.getContentPane().add(panTextAndImg, BorderLayout.CENTER);
		frame.getContentPane().add(buttonClear, BorderLayout.SOUTH);
		frame.setVisible(true);

		frame.addWindowListener(this);
		// buttonClear.addActionListener(this);

		// kp: This try-catch is to redirect standard output to first
		// PipedOutputStream named "pout"
		// kp: and that "pout" stream is piped/passed to "pin" (pout is sender,
		// pin is receiver)
		try {
			// kp:
			// http://www.java2s.com/Tutorials/Java/Java_io/0300__Java_io_Pipe.htm
			// kp: 'pout' constructed and then connected to the other end of the
			// pipe of which one end is 'pin'
			PipedOutputStream pout = new PipedOutputStream(this.pin);

			// https://www.tutorialspoint.com/java/lang/system_setout.htm (see
			// setOut used on FileOutputStream)
			// Passing/redirecting standard output stream to "pout"
			System.setOut(new PrintStream(pout, true));
		} catch (java.io.IOException io) {
			textArea.append("Couldn't redirect STDOUT to this console\n" + io.getMessage());
		} catch (SecurityException se) {
			textArea.append("Couldn't redirect STDOUT to this console\n" + se.getMessage());
		}

		// kp: This try-catch is to redirect standard errors (or err-stream) to
		// second PipedOutputStream named "pout2"
		// kp: and that "pout2" stream is piped/passed to "pin2" (pout2 is
		// sender, pin2 is receiver)
		try {
			PipedOutputStream pout2 = new PipedOutputStream(this.pin2);
			System.setErr(new PrintStream(pout2, true));
		} catch (java.io.IOException io) {
			textArea.append("Couldn't redirect STDERR to this console\n" + io.getMessage());
		} catch (SecurityException se) {
			textArea.append("Couldn't redirect STDERR to this console\n" + se.getMessage());
		}

		quit = false; // signals the Threads that they should exit

		// Starting two separate threads to read from the PipedInputStreams
		//
		// kp: Following reader, and reader2 are instances (objects) of this
		// class (i.e. aSimpleJavaConsole) itself
		reader = new Thread(this);
		reader.setDaemon(true); // kp: make this thread a process running in the
		                        // background (no interactive access)
		reader.start(); // kp: start this process
		//
		reader2 = new Thread(this);
		reader2.setDaemon(true);
		reader2.start();

		/*
		 * // testing part // you may omit this part for your application // System.out.println("Hello World 2");
		 * System.out.println("All fonts available to Graphic2D:\n"); GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment(); String[]
		 * fontNames=ge.getAvailableFontFamilyNames(); for(int n=0;n<fontNames.length;n++) System.out.println(fontNames[n]); // Testing part: simple an error thrown
		 * anywhere in this JVM will be printed on the Console // We do it with a seperate Thread becasue we don't wan't to break a Thread used by the Console.
		 * 
		 * System.out.println("\nLets throw an error on this console"); errorThrower=new Thread(this); errorThrower.setDaemon(true); errorThrower.start();
		 */
	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	protected static ImageIcon createImageIcon(String path) {
		// java.net.URL imgURL = FileChooserDemo.class.getResource(path);
		java.net.URL imgURL = DC_CalibrationNew.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	public synchronized void windowClosed(WindowEvent evt) {
		quit = true;
		this.notifyAll(); // stop all threads (kp: notify All threads?)
		// kp:
		// https://docs.oracle.com/javase/tutorial/essential/concurrency/join.html
		try {
			reader.join(1000);
			pin.close();
		} catch (Exception e) {
		}
		try {
			reader2.join(1000);
			pin2.close();
		} catch (Exception e) {
		}
		System.exit(0);
	}

	public synchronized void windowClosing(WindowEvent evt) {
		frame.setVisible(false); // default behaviour of JFrame
		frame.dispose();
	}

	public synchronized void actionPerformed(ActionEvent evt) {
		// Handle open button action.
		if (evt.getSource() == bFileChooser) {
			int returnVal = fc.showOpenDialog(fc);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				try {
					fileName = file.getCanonicalPath();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// This is where a real application would open the file.
				// log.append("Opening: " + file.getName() + "." + newline);
				System.out.println("Opening: " + fileName + ".\n");
			} else {
				// log.append("Open command cancelled by user." + newline);
				System.out.println("Open command cancelled by user.\n");
			}
			// log.setCaretPosition(log.getDocument().getLength());

			// Handle save button action.
		} /*
		   * else if (evt.getSource() == saveButton) { int returnVal = fc.showSaveDialog(FileChooserDemo.this); if (returnVal == JFileChooser.APPROVE_OPTION) { File
		   * file = fc.getSelectedFile(); //This is where a real application would save the file. log.append("Saving: " + file.getName() + "." + newline); } else {
		   * log.append("Save command cancelled by user." + newline); } log.setCaretPosition(log.getDocument().getLength()); }
		   */
		else if (evt.getSource() == buttonClear) {
			textArea.setText("");
		}
	}

	public synchronized void run() {
		try {
			while (Thread.currentThread() == reader) {
				try {
					this.wait(100);
				} catch (InterruptedException ie) {
				}
				if (pin.available() != 0) {
					String input = this.readLine(pin);
					textArea.append(input);
				}
				if (quit)
					return;
			}

			while (Thread.currentThread() == reader2) {
				try {
					this.wait(100);
				} catch (InterruptedException ie) {
				}
				if (pin2.available() != 0) {
					String input = this.readLine(pin2);
					textArea.append(input);
				}
				if (quit)
					return;
			}
		} catch (Exception e) {
			textArea.append("\nConsole reports an Internal error.");
			textArea.append("The error is: " + e);
		}

		/*
		 * // just for testing (Throw a Nullpointer after 1 second) if (Thread.currentThread()==errorThrower) { try { this.wait(1000); }catch(InterruptedException
		 * ie){} throw new NullPointerException( "Application test: throwing an NullPointerException It should arrive at the console" ); }
		 */
	}

	public synchronized String readLine(PipedInputStream in) throws IOException {
		String input = "";
		do {
			/**
			 * kp: PipedInputStream inherits from InputStream and available() is one of its methods. https://docs.oracle.com/javase/7/docs/api/java/io/InputStream.
			 * html available(): Returns an estimate of the number of bytes that can be read (or skipped over) from this input stream without blocking by the next
			 * invocation of a method for this input stream.
			 * 
			 * read(byte[] b): Reads some number of bytes from the input stream and stores them into the buffer array b.
			 */
			int available = in.available();
			if (available == 0)
				break;
			byte b[] = new byte[available]; // kp: creating a 'byte' array of
			                                // size 'available'
			in.read(b);
			input = input + new String(b, 0, b.length);
		} while (!input.endsWith("\n") && !input.endsWith("\r\n") && !quit);
		return input;
	}

	// private void initMenu() {
	// JMenuBar bar = new JMenuBar();
	// JMenu fileMenu = new JMenu("File");
	// JMenuItem processItem = new JMenuItem("Process File");
	// processItem.addActionListener(this);
	// fileMenu.add(processItem);
	// bar.add(fileMenu);
	//
	// JMenu pluginMenu = new JMenu("Plugins");
	// JMenuItem loadPlugin = new JMenuItem("Tree Viewer");
	// loadPlugin.addActionListener(this);
	// pluginMenu.add(loadPlugin);
	//
	// bar.add(pluginMenu);
	//
	// this.setJMenuBar(bar);
	// }

}