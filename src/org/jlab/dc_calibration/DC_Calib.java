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
import java.awt.GridLayout;
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

public class DC_Calib extends WindowAdapter implements WindowListener, ActionListener, Runnable {

	private JFrame frame;
	private JTextArea textArea;
	protected Thread reader, reader2;
	private boolean quit;

	private final PipedInputStream pin = new PipedInputStream();
	private final PipedInputStream pin2 = new PipedInputStream();

	Thread errorThrower; // just for testing (Throws an Exception at this
	                     // Console
	Thread mythread;
	// Banner
	private JLabel banner;
	// JPanels to be used
	private JPanel bannerPanel, panelForWelcomeAndOpenFile, panelForFileOpen, panelImg, centerPanel;
	private int gridSize = 3;
	private JPanel buttonPanel;
	// a file chooser to be used to open file to analyze
	JFileChooser fc;
	// file to be read and analyzed
	private String fileName;
	// buttons to be implemented
	JButton bFileChooser, bTestEvent, bReadRecDataIn, bReconst, bReadRecDataForMinuit, ccdbWriter, buttonClear;

	public DC_Calib() {
		createFrame();
		createFileChooser();
		createButtons();
		createPanels();
		initFrame();
	}

	private void createFrame() {
		// create all components and add them
		frame = new JFrame("DC Calibration Console");
		frame.setLayout(new BorderLayout());// kp

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = new Dimension((int) (screenSize.width / 1.25), (int) (screenSize.height / 1.5));
		int x = (int) (frameSize.width / 2); // kp: quarter of screen width
		int y = (int) (frameSize.height / 2);// kp: quarter of screen height
		frame.setBounds(x, y, frameSize.width, frameSize.height);
	}

	private void createBanner() {
		banner = new JLabel("Welcome to DC Calibration for CLAS12!", JLabel.CENTER);
		banner.setForeground(Color.yellow);
		banner.setBackground(Color.gray);
		banner.setOpaque(true);
		banner.setFont(new Font("SansSerif", Font.BOLD, 20));
		banner.setPreferredSize(new Dimension(1000, 30));
	}

	private void createFileChooser() {
		fc = new JFileChooser();
	}

	private void createButtons() {
		bFileChooser = new JButton("Choose File", createImageIcon("/images/Open16.gif"));
		bTestEvent = new JButton();
		bReadRecDataIn = new JButton();
		bReconst = new JButton();
		bReadRecDataForMinuit = new JButton();
		ccdbWriter = new JButton();
		buttonClear = new JButton("Clear");

		bTestEvent.setText("<html>" + "Estimate & Apply T0 Correction" + "</html>");
		bReadRecDataIn.setText("<html>" + "Run Decoder" + "</html>");
		bReconst.setText("<html>" + "Run Reconstruction" + "</html>");
		bReadRecDataForMinuit.setText("<html>" + "Run DOCA-to-Time Fitter" + "</html>");
		ccdbWriter.setText("<html>" + "Send Results to CCDB" + "</html>");
	}

	private void createPanels() {
		bannerPanel = new JPanel(new BorderLayout());
		addToBanner();

		panelForFileOpen = new JPanel(new BorderLayout());
		panelForFileOpen.setBorder(BorderFactory.createEtchedBorder());
		addToOpenFilePanel();

		panelForWelcomeAndOpenFile = new JPanel(new BorderLayout());
		addToWelcomePanel();

		panelImg = new JPanel(new BorderLayout());
		addToPanelImage();

		buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(gridSize, gridSize, 1, 1));
		addToButtonPanel();

		centerPanel = new JPanel(new BorderLayout());
		addToCenterPanel();

	}

	private void addToBanner() {
		createBanner();
		bannerPanel.add(banner, BorderLayout.CENTER);
	}

	private void addToOpenFilePanel() {
		panelForFileOpen.add(bFileChooser, BorderLayout.LINE_START);
	}

	private void addToWelcomePanel() {
		panelForWelcomeAndOpenFile.add(bannerPanel, BorderLayout.NORTH);
		panelForWelcomeAndOpenFile.add(panelForFileOpen, BorderLayout.SOUTH);
	}

	private void addToPanelImage() {
		ImageIcon imageIcon = new ImageIcon(new ImageIcon(this.getClass().getResource("/images/CLAS12.jpg")).getImage()
		        .getScaledInstance(320, 320, java.awt.Image.SCALE_SMOOTH));
		// ImageIcon(this.getClass().getResource("images/timeVsTrkDoca_and_Profiles.png"));
		JLabel imgLabel = new JLabel(imageIcon);
		panelImg.add(imgLabel, BorderLayout.CENTER);
	}

	private void addToPanelImage(String whoMadeMeWake) {
		ImageIcon imageIcon = new ImageIcon(new ImageIcon(this.getClass().getResource("/images/CLAS12.jpg")).getImage()
		        .getScaledInstance(320, 320, java.awt.Image.SCALE_SMOOTH));
		// ImageIcon(this.getClass().getResource("images/timeVsTrkDoca_and_Profiles.png"));
		JLabel imgLabel = new JLabel(imageIcon);
		panelImg.add(imgLabel, BorderLayout.CENTER);
	}

	private void addToButtonPanel() {
		buttonPanel.add(bReadRecDataIn);
		buttonPanel.add(bReconst);
		buttonPanel.add(bTestEvent);
		buttonPanel.add(bReadRecDataForMinuit);
		buttonPanel.add(ccdbWriter);
	}

	private void addToCenterPanel() {
		addToTextArea();
		centerPanel.add(new JScrollPane(panelImg), BorderLayout.WEST);
		centerPanel.add(buttonPanel, BorderLayout.CENTER);
		centerPanel.add(new JScrollPane(textArea), BorderLayout.EAST);

	}

	private void addToTextArea() {
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setPreferredSize(new Dimension(300, 380));
		textArea.setLineWrap(true);
		// done below
	}

	private void initFrame() {

		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(panelForWelcomeAndOpenFile, BorderLayout.NORTH);
		frame.getContentPane().add(centerPanel, BorderLayout.CENTER);
		frame.getContentPane().add(buttonClear, BorderLayout.SOUTH);
		frame.setVisible(true);

		addListeners();

	}

	private void addListeners() {
		bFileChooser.addActionListener(this);

		TestEvent e1 = new TestEvent();
		bTestEvent.addActionListener(e1);
		ReadRecDataIn e2 = new ReadRecDataIn();// fileName
		bReadRecDataIn.addActionListener(e2);
		ReadRecDataForMinuit e3 = new ReadRecDataForMinuit();
		bReadRecDataForMinuit.addActionListener(e3);
		buttonClear.addActionListener(this);
		listen();
	}

	private void listen() {
		frame.addWindowListener(this);
		try {
			PipedOutputStream pout = new PipedOutputStream(this.pin);

			System.setOut(new PrintStream(pout, true));
		} catch (java.io.IOException io) {
			textArea.append("Couldn't redirect STDOUT to this console\n" + io.getMessage());
		} catch (SecurityException se) {
			textArea.append("Couldn't redirect STDOUT to this console\n" + se.getMessage());
		}
		try {
			PipedOutputStream pout2 = new PipedOutputStream(this.pin2);
			System.setErr(new PrintStream(pout2, true));
		} catch (java.io.IOException io) {
			textArea.append("Couldn't redirect STDERR to this console\n" + io.getMessage());
		} catch (SecurityException se) {
			textArea.append("Couldn't redirect STDERR to this console\n" + se.getMessage());
		}

		quit = false; // signals the Threads that they should exit
		reader = new Thread(this);
		reader.setDaemon(true); // kp: make this thread a process running in the
		                        // background (no interactive access)
		reader.start(); // kp: start this process
		//
		reader2 = new Thread(this);
		reader2.setDaemon(true);
		reader2.start();
	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	protected static ImageIcon createImageIcon(String path) {
		// java.net.URL imgURL = FileChooserDemo.class.getResource(path);
		ImageIcon myImageIcon;
		java.net.URL imgURL = DC_Calib.class.getResource(path);
		if (imgURL != null) {
			myImageIcon = new ImageIcon(imgURL);
			return myImageIcon;
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
					this.fileName = file.getCanonicalPath();
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Opening: " + fileName + "\n");
			} else {
				// log.append("Open command cancelled by user." + newline);
				System.out.println("Open command cancelled by user.\n");
			}
		} else if (evt.getSource() == buttonClear) {
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
