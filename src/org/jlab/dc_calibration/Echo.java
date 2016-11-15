/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.dc_calibration;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;

/**
 *
 * @author KPAdhikari
 */
// http://stackoverflow.com/questions/4422642/java-console-jpanel
public class Echo extends FilterOutputStream {
	private final JTextArea text;

	public Echo(OutputStream out, JTextArea text) {
		// public Echo( DC_calibration out, JTextArea text) {
		super(out);
		if (text == null)
			throw new IllegalArgumentException("null text");
		this.text = text;
	}

	@Override
	public void write(int b) throws IOException {
		super.write(b);
		text.append(Character.toString((char) b));
		// scroll to end?
	}

	// overwrite the other write methods for better performance
}
