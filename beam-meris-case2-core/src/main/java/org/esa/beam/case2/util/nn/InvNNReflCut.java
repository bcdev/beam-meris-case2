package org.esa.beam.case2.util.nn;

import java.io.IOException;


/**
 * @author schiller
 *
 */
public class InvNNReflCut extends NNffbpAlphaTabFast
{
	double reflCut;
	public int count;
	
	public InvNNReflCut(String netname, double reflCut)  throws IOException
	{
		super(netname);
		this.reflCut = reflCut;
	}
	
	public double[] calc(double[] nninp) 
	{
		this.count = 0;
		for(int i=0;i<8;i++) 
		{
			if(nninp[3+i]  < this.reflCut) 
			{
				nninp[3+i]  = this.reflCut;
				this.count++;
			}
		}
		return super.calc(nninp);
	}

}
