package org.esa.beam.case2.util.nn;

import java.io.IOException;

/**
 * @author schiller
 *
 */
public class ForwNNReflCut extends NNffbpAlphaTabFast
{
	double reflCut;

	public ForwNNReflCut(String netname, double reflCut)  throws IOException
	{
		super(netname);
		this.reflCut = reflCut;
	}
	
	public double[] calc(double[] nninp) 
	{
		double[] nnout = super.calc(nninp);
		for(int i=0;i<nnout.length;i++) 
		{
			if(nnout[i]  < this.reflCut) 
			{
				nnout[i]  = this.reflCut;
			}
		}
		return nnout;
	}
	
	public NNCalc calcJacobi(double[] nnInp)
	{
		NNCalc nnCalc = super.calcJacobi(nnInp);
		for(int i=0;i<nnCalc.nnOutput.length;i++)
		{
			if(nnCalc.nnOutput[i]  < this.reflCut) 
			{
				nnCalc.nnOutput[i]  = this.reflCut;
			}
		}
		return nnCalc;
	}

}
