package bioasq.ml

import java.lang.Math._

/**
 *
 */

object UnivariateTests {

  /**
   * give a lower bound on an estimate for the log-ratio between two binomial proportions.  (smaller magnitude).
   *
   * p1 = positiveWithFeature / numWithFeature (rate of positives among sample w/ some feature present)
   * p2 = positives / sampleSize (base rate)
   *
   * The lower bound is based on a confidence interval.  This in turn relies on a normal approximation for the
   * ratio of two binomial proportions. Rule of thumb is that its reasonable when the counts > 7 or so.
   *
   */
  def binomialRatioConfidence(positiveWithFeature: Double, numWithFeature: Double, positives: Double, sampleSize: Double, stdErrorInterval : Double = 1.96): Double = {
    //see eg. http://onbiostatistics.blogspot.com/2012/08/confidence-intervals-for-difference.html
    // though real reference is Agresti A (2007) An Introduction to Categorical Data Analysis, 2nd edition, JohnWiley & Sons, Inc.,
    val adjPF = if (positiveWithFeature == 0) 0.5 else positiveWithFeature
    val p1 = adjPF / numWithFeature
    val p2 = positives / sampleSize
    val stdErr = sqrt((1-p1)/(numWithFeature) + (1-p2)/(sampleSize))
    val rawRatio = log(p1 / p2)
    val neg = rawRatio < 0
    val absRawRatio = if (neg) -rawRatio else rawRatio
    val penalty = stdErr * stdErrorInterval
    if (penalty >= absRawRatio)
      0
    else {
      val t = absRawRatio - penalty
      if (neg) -t else t
    }
  }

}
