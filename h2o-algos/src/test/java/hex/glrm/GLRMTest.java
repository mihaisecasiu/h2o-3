package hex.glrm;

import hex.DataInfo;
import hex.glrm.GLRMModel.GLRMParameters;
import hex.svd.SVD;
import hex.svd.SVDModel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import water.DKV;
import water.Key;
import water.Scope;
import water.TestUtil;
import water.fvec.Frame;
import water.util.ArrayUtils;
import water.util.FrameUtils;
import water.util.Log;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

public class GLRMTest extends TestUtil {
  public final double TOLERANCE = 1e-6;
  @BeforeClass public static void setup() { stall_till_cloudsize(1); }

  public double errStddev(double[] expected, double[] actual) {
    double err = 0;
    for(int i = 0; i < actual.length; i++) {
      double diff = expected[i] - actual[i];
      err += diff * diff;
    }
    return err;
  }
  public void checkStddev(double[] expected, double[] actual) { checkStddev(expected, actual, TOLERANCE); }
  public void checkStddev(double[] expected, double[] actual, double threshold) {
    for(int i = 0; i < actual.length; i++)
      Assert.assertEquals(expected[i], actual[i], threshold);
  }

  public double errEigvec(double[][] expected, double[][] actual) { return errEigvec(expected, actual, TOLERANCE); }
  public double errEigvec(double[][] expected, double[][] actual, double threshold) {
    double err = 0;
    for(int j = 0; j < actual[0].length; j++) {
      boolean flipped = Math.abs(expected[0][j] - actual[0][j]) > threshold;
      for(int i = 0; i < actual.length; i++) {
        double diff = expected[i][j] - (flipped ? -actual[i][j] : actual[i][j]);
        err += diff * diff;
      }
    }
    return err;
  }
  public boolean[] checkEigvec(double[][] expected, double[][] actual) { return checkEigvec(expected, actual, TOLERANCE); }
  public boolean[] checkEigvec(double[][] expected, double[][] actual, double threshold) {
    int nfeat = actual.length;
    int ncomp = actual[0].length;
    boolean[] flipped = new boolean[ncomp];

    for(int j = 0; j < ncomp; j++) {
      flipped[j] = Math.abs(expected[0][j] - actual[0][j]) > threshold;
      for(int i = 0; i < nfeat; i++) {
        Assert.assertEquals(expected[i][j], flipped[j] ? -actual[i][j] : actual[i][j], threshold);
      }
    }
    return flipped;
  }

  @Test public void testArrests() throws InterruptedException, ExecutionException {
    GLRM job = null;
    GLRMModel model = null;
    Frame train = null;
    try {
      train = parse_test_file(Key.make("arrests.hex"), "smalldata/pca_test/USArrests.csv");
      GLRMParameters parms = new GLRMParameters();
      parms._train = train._key;
      parms._k = 4;
      parms._gamma = 0;
      parms._transform = DataInfo.TransformType.NONE;
      parms._recover_pca = false;

      try {
        job = new GLRM(parms);
        model = job.trainModel().get();
        Log.info("Iteration " + model._output._iterations + ": Objective value = " + model._output._objective);
      } catch (Throwable t) {
        t.printStackTrace();
        throw new RuntimeException(t);
      } finally {
        job.remove();
      }
    } catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException(t);
    } finally {
      if (train != null) train.delete();
      if (model != null) {
        model._parms._loading_key.get().delete();
        model.delete();
      }
    }
  }

  @Test public void testBenign() throws InterruptedException, ExecutionException {
    GLRM job = null;
    GLRMModel model = null;
    Frame train = null;
    try {
      train = parse_test_file(Key.make("benign.hex"), "smalldata/logreg/benign.csv");
      GLRMParameters parms = new GLRMParameters();
      parms._train = train._key;
      parms._k = 10;
      parms._gamma = 0;
      parms._transform = DataInfo.TransformType.STANDARDIZE;
      parms._recover_pca = false;

      try {
        job = new GLRM(parms);
        model = job.trainModel().get();
        Log.info("Iteration " + model._output._iterations + ": Objective value = " + model._output._objective);
      } catch (Throwable t) {
        t.printStackTrace();
        throw new RuntimeException(t);
      } finally {
        job.remove();
      }
    } catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException(t);
    } finally {
      if (train != null) train.delete();
      if (model != null) {
        model._parms._loading_key.get().delete();
        model.delete();
      }
    }
  }

  @Test public void testArrestsPCA() throws InterruptedException, ExecutionException {
    // Initialize using first k rows of standardized training frame
    Frame yinit = frame(ard(ard(1.24256408, 0.7828393, -0.5209066, -0.003416473),
            ard(0.50786248, 1.1068225, -1.2117642, 2.484202941),
            ard(0.07163341, 1.4788032, 0.9989801, 1.042878388),
            ard(0.23234938, 0.2308680, -1.0735927, -0.184916602)));
    double[] stddev = new double[] {1.5748783, 0.9948694, 0.5971291, 0.4164494};
    double[][] eigvec = ard(ard(-0.5358995, 0.4181809, -0.3412327, 0.64922780),
            ard(-0.5831836, 0.1879856, -0.2681484, -0.74340748),
            ard(-0.2781909, -0.8728062, -0.3780158, 0.13387773),
            ard(-0.5434321, -0.1673186, 0.8177779, 0.08902432));

    GLRMModel model = null;
    Frame train = null;
    try {
      train = parse_test_file(Key.make("arrests.hex"), "smalldata/pca_test/USArrests.csv");
      GLRMParameters parms = new GLRMParameters();
      parms._train = train._key;
      parms._k = 4;
      parms._gamma = 0;
      parms._transform = DataInfo.TransformType.STANDARDIZE;
      parms._max_iterations = 1000;
      parms._user_points = yinit._key;
      parms._recover_pca = true;

      GLRM job = new GLRM(parms);
      try {
        model = job.trainModel().get();
        checkStddev(stddev, model._output._std_deviation);
        checkEigvec(eigvec, model._output._eigenvectors_raw);
      } catch (Throwable t) {
        t.printStackTrace();
        throw new RuntimeException(t);
      } finally {
        job.remove();
      }
    } catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException(t);
    } finally {
      yinit.delete();
      if (train != null) train.delete();
      if (model != null) {
        model._parms._loading_key.get().delete();
        model.delete();
      }
    }
  }

  @Test public void testArrestsMissing() throws InterruptedException, ExecutionException {
    // Expected eigenvectors and their standard deviations with standardized data
    double[] stddev = new double[] {1.5748783, 0.9948694, 0.5971291, 0.4164494};
    double[][] eigvec = ard(ard(-0.5358995, 0.4181809, -0.3412327, 0.64922780),
            ard(-0.5831836, 0.1879856, -0.2681484, -0.74340748),
            ard(-0.2781909, -0.8728062, -0.3780158, 0.13387773),
            ard(-0.5434321, -0.1673186, 0.8177779, 0.08902432));

    long seed = 1234;
    GLRMModel model = null;
    Frame train = null;
    GLRMParameters parms;
    Map<Double,Double> sd_map = new TreeMap<>();
    Map<Double,Double> ev_map = new TreeMap<>();
    StringBuilder sb = new StringBuilder();

    for (double missing_fraction : new double[]{0, 0.1, 0.25, 0.5, 0.75, 0.9}) {
      try {
        Scope.enter();
        train = parse_test_file(Key.make("arrests.hex"), "smalldata/pca_test/USArrests.csv");

        // Add missing values to the training data
        if (missing_fraction > 0) {
          Frame frtmp = new Frame(Key.make(), train.names(), train.vecs());
          DKV.put(frtmp._key, frtmp); // Need to put the frame (to be modified) into DKV for MissingInserter to pick up
          FrameUtils.MissingInserter j = new FrameUtils.MissingInserter(frtmp._key, seed, missing_fraction);
          j.execImpl();
          j.get(); // MissingInserter is non-blocking, must block here explicitly
          DKV.remove(frtmp._key); // Delete the frame header (not the data)
        }

        parms = new GLRMParameters();
        parms._train = train._key;
        parms._k = train.numCols();
        parms._gamma = 0;
        parms._transform = DataInfo.TransformType.STANDARDIZE;
        parms._max_iterations = 1000;
        parms._seed = seed;
        parms._recover_pca = true;

        GLRM job = new GLRM(parms);
        try {
          model = job.trainModel().get();
          Log.info(100 * missing_fraction + "% missing values: Objective = " + model._output._objective);
          double sd_err = errStddev(stddev, model._output._std_deviation)/parms._k;
          double ev_err = errEigvec(eigvec, model._output._eigenvectors_raw)/parms._k;
          Log.info("Avg SSE in Std Dev = " + sd_err + "\tAvg SSE in Eigenvectors = " + ev_err);
          sd_map.put(missing_fraction, sd_err);
          ev_map.put(missing_fraction, ev_err);
        } catch (Throwable t) {
          t.printStackTrace();
          throw new RuntimeException(t);
        } finally {
          job.remove();
        }
        Scope.exit();
      } catch(Throwable t) {
        t.printStackTrace();
        throw new RuntimeException(t);
      } finally {
        if (train != null) train.delete();
        if (model != null) {
          model._parms._loading_key.get().delete();
          model.delete();
        }
      }
    }
    sb.append("Missing Fraction --> Avg SSE in Std Dev\n");
    for (String s : Arrays.toString(sd_map.entrySet().toArray()).split(",")) sb.append(s.replace("=", " --> ")).append("\n");
    sb.append("\n");
    sb.append("Missing Fraction --> Avg SSE in Eigenvectors\n");
    for (String s : Arrays.toString(ev_map.entrySet().toArray()).split(",")) sb.append(s.replace("=", " --> ")).append("\n");
    Log.info(sb.toString());
  }

  @Test public void testPowerMethod() {
    double[][] svec = ard(ard(-0.04239181, 0.01616262, -0.06588426, 0.99679535),
                          ard(-0.94395706, 0.32068580, 0.06655170, -0.04094568),
                          ard(-0.30842767, -0.93845891, 0.15496743, 0.01234261),
                          ard(-0.10963744, -0.12725666, -0.98347101, -0.06760284));
    SVDModel model = null;
    Frame train = null;
    try {
      train = parse_test_file(Key.make("arrests.hex"), "smalldata/pca_test/USArrests.csv");
      SVDModel.SVDParameters parms = new SVDModel.SVDParameters();
      parms._train = train._key;
      parms._k = 4;
      parms._seed = 1234;

      SVD job = new SVD(parms);
      try {
        model = job.trainModel().get();
        for(int i = 0; i < parms._k; i++)
          Assert.assertArrayEquals(svec[i], model._output._v[i], TOLERANCE);
      } catch (Throwable t) {
        t.printStackTrace();
        throw new RuntimeException(t);
      } finally {
        job.remove();
      }
    } catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException(t);
    } finally {
      if (train != null) train.delete();
    }
  }
}