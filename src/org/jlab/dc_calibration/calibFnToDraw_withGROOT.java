/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.dc_calibration;

//kp: 8/15/16: Started from the copy of Will Phelp's code for doubleGaussianF1D class that he had written for me.
import java.util.ArrayList;

import org.jlab.groot.math.Func1D;

/**
 *
 * @author KPAdhikari
 */

// public class calibFnToDraw_withGROOT extends F1D {
public class calibFnToDraw_withGROOT extends Func1D {
	int npx = 1000;
	public static final double rad2deg = 180.0 / Math.PI;
	public static final double cos30 = Math.cos(30.0 / rad2deg);

	// public doubleGaussianF1D() {
	public calibFnToDraw_withGROOT() {
		super("calibFnToDraw", 0.0, 1.0); // super("doubleGaussian",0.0,1.0);
		this.initParameters();
	}

	// public doubleGaussianF1D(String name, double xmin, double xmax){
	public calibFnToDraw_withGROOT(String name, double xmin, double xmax) {
		super(name, xmin, xmax);
		initParameters();
	}

	private void initParameters() {
		ArrayList<String> pars = new ArrayList<String>();
		pars.add("v0");
		pars.add("deltamn");
		pars.add("tmax1");
		pars.add("tmax2");
		pars.add("distbeta");
		pars.add("SL"); // This is not a fit-par //Values of SL are 1,2,3 ..,6
		pars.add("thetaDeg"); // This is not a fit-par
		pars.add("docaMax"); // This is not a fit-par //2*wpdist[SL-1]
		// docaMax comes from 2*wpdist[SL-1], but eventually I want to make it
		// CCDB dependent
		// by making the main program CCDB dependent and getting its value as
		// input par.
		// that's why I chose it as one of the input pars, although I could
		// derive it from SL.
		double prevFitPars[] = { 62.92e-04, 1.35, 137.67, 148.02, 0.055, 1.0, 0.0, 0.3861 };

		/*
		 * this.setNParams(pars.size()); //Valid for old F1D (coatjava 2.4)
		 * for(int loop = 0; loop < pars.size(); loop++){
		 * this.parameter(loop).setName(pars.get(loop));
		 * this.parameter(loop).setValue(prevFitPars[loop]); }
		 */

		for (int loop = 0; loop < pars.size(); loop++) {
			this.addParameter(pars.get(loop)); // This is mandatory, else we get
												// java.lang.IndexOutOfBoundsException
			// this.setParameter(loop,prevFitPars[loop]); //Works, but
			// this.setParameters(prevFitPars); below also works
		}
		this.setParameters(prevFitPars); // Valid for Func1D (groot) (see
											// following link)
		// https://github.com/gavalian/groot/blob/master/src/main/java/org/jlab/groot/math/Func1D.java

	}

	@Override
	public void setParameters(double[] params) {
		for (int i = 0; i < params.length; i++) {
			this.setParameter(i, params[i]);
			println("We set " + this.parameter(i).value());
		}
	}

	@Override
	// public double eval(double x){
	// public double eval(double xNorm){
	public double evaluate(double xNorm) {
		double v0 = this.parameter(0).value();
		double deltamn = this.parameter(1).value();
		double tmax1 = this.parameter(2).value();
		double tmax2 = this.parameter(3).value();
		double distbeta = this.parameter(4).value();
		double fSL = this.parameter(5).value();// Values of SL are 1,2,3 ..,6
		double thetaDeg = this.parameter(6).value();
		double docaMax = this.parameter(7).value();

		int SL = (int) fSL;
		// final double rad2deg = 180.0/Math.PI;

		// Many lines copied from calcTimeFunc(..) function in the main class
		// file
		double Dc = docaMax * cos30; // docaMax = 2*wpdist[SL-1],

		// double X=docaByDocaMax, x=X*dMax, Xhat0 = X/cos30;
		double x = xNorm * docaMax; // argument xNorm is actually x/docaMax,
									// below we want real x
		double Xhat0 = x / Dc, deltanm = deltamn;
		// double v0Par = par[0], deltanm = par[1], tMax = par[2]; if(SL==2)
		// tMax = par[3];
		// double distbeta = par[4]; //8/3/16: initial value given by Mac is
		// 0.050 cm.
		double v0Par = v0, tMax = tmax1;
		if (SL == 2)
			tMax = tmax2;

		// Assume a functional form (time = x/v0+a*(x/dmax)**n+b*(x/dmax)**m)
		// for theta = 30 deg.
		// First, calculate n
		double nPar = (1.0 + (deltanm - 1.0) * Math.pow(0.615, deltanm)) / (1.0 - Math.pow(0.615, deltanm));

		// now, calculate m
		double mPar = nPar + deltanm;// Actually it should be named deltamn and
										// should be + in between //7/21/16
		// determine b from the constraint that the time = tmax at dist=dmax
		double b = (tMax - docaMax / v0Par) / (1.0 - mPar / nPar);

		// determine a from the requirement that the derivative at
		// d=dmax equal the derivative at d=0
		double a = -b * mPar / nPar; // From one of the constraints
		double alpha = thetaDeg; // = 0.0; //Local angle in degrees.
		double cos30minusalpha = Math.cos((30. - alpha) / rad2deg); // =Math.cos(Math.toRadians(30.-alpha));
		double xhat = x / docaMax, dmaxalpha = docaMax * cos30minusalpha, xhatalpha = x / dmaxalpha;

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
		double term1 = x / v0, term2 = a * xhatPowN, term3 = balpha * xhatPowM;
		// double time = x/v0Par + a*pow(xhat, nPar) + balpha*pow(xhat, mPar);

		// 8/3/16 ===== additing new contribution from beta-dependent time walk
		// (with 5th par 'distbeta')
		// ===================== 8/3/16
		double beta = 1.0;// , distbeta = par[5];
		// double deltatime_beta=(sqrt(x**2+(distbeta*beta**2)**2)-x)/v0;
		// //where x is trkdoca
		double deltatime_beta = (Math.sqrt(x * x + Math.pow(distbeta * Math.pow(beta, 2), 2)) - x) / v0Par;
		// time = time + deltatime_beta;
		// ===================== 8/3/16

		double calcTime = term1 + term2 + term3 + deltatime_beta;
		return calcTime;
	}

	/*
	 * //Only valid for extending old F1D, not valid for Func1D (10/5/16)
	 * 
	 * @Override public double getChiSquare(IDataSet ds,String options){ return
	 * 0.0; }
	 * 
	 * @Override public double getChiSquare(IDataSet ds){ return 0.0; }
	 */
	// Some frequently used methods (may be better to put it in a separate file)
	public static void println(String str) {
		System.out.println(str);
	}

	public static void print(String str) {
		System.out.print(str);
	}
}