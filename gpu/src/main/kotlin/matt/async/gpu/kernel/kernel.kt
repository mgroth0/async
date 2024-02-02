package matt.async.gpu.kernel

import com.aparapi.Kernel
import com.aparapi.Range

class kernel(private val m1: DoubleArray, private val m2: DoubleArray) {

    private var kernel: Kernel? = null

    fun calc(): Double {
        val localM1 = m1
        val localM2 = m2
        val preResult = DoubleArray(m1.size)
        kernel = object: Kernel() {
            override fun run() {
                val i = globalId
                preResult[i] = localM1[i]*localM2[i]
            }
        }
        kernel!!.execute(Range.create(preResult.size))
        var r = 0.0
        preResult.forEach {
            r += it
        }
        return r
    }
}


/*JAVA:*/
/*


public class kernel {

    double[] m1;
    double[] m2;
    Kernel kernel;

    public kernel(double[] m1, double[] m2) {
        this.m1 = m1;
        this.m2 = m2;
    }

    public double calc() {
        double[] localM1 = m1;
        double[] localM2 = m2;
        double[] preResult = new double[m1.length];
        kernel = new Kernel() {
            @Override
            public void run() {
                int i = getGlobalId();
                preResult[i] = localM1[i] * localM2[i];
            }
        };
        kernel.execute(Range.create(preResult.length));
        double r = 0.0;
        for (double rr : preResult) {
            r += rr;
        }
        return r;
    }
}



* */
