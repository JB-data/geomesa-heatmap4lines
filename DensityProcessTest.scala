/***********************************************************************
 * Copyright (c) 2013-2022 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.process.analytic

import org.geotools.data.Query
import org.geotools.feature.DefaultFeatureCollection
import org.geotools.filter.text.ecql.ECQL
import org.junit.runner.RunWith
import org.locationtech.geomesa.accumulo.TestWithFeatureType
import org.locationtech.geomesa.features.ScalaSimpleFeature
import org.locationtech.geomesa.index.iterators.StatsScan
import org.locationtech.geomesa.utils.collection.SelfClosingIterator
import org.locationtech.geomesa.utils.stats._
import org.opengis.filter.Filter
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import java.util.Collections
//added JB
import org.locationtech.geomesa.utils.geotools.{FeatureUtils, SimpleFeatureTypes}
import org.geotools.geometry.jts.ReferencedEnvelope
import org.locationtech.geomesa.utils.geotools.{CRS_EPSG_4326, FeatureUtils}
import org.geotools.referencing.CRS

@RunWith(classOf[JUnitRunner])
class DensityProcessTest extends Specification with TestWithFeatureType {
  sequential

  override val spec = "an_id:java.lang.Integer,attr:java.lang.Long,dtg:Date,*geom:Point:srid=4326"

  addFeatures((0 until 150).toArray.map { i =>
    val attrs = Array(i.asInstanceOf[AnyRef], (i * 2).asInstanceOf[AnyRef], "2012-01-01T19:00:00Z", "POINT(-77 38)")
    val sf = new ScalaSimpleFeature(sft, i.toString)
    sf.setAttributes(attrs)
    sf
  })

  def query = new Query(sftName, ECQL.toFilter("dtg DURING 2012-01-01T18:30:00.000Z/2012-01-01T19:30:00.000Z " +
    "AND bbox(geom,-80,35,-75,40)"))




/* this is the example that I would want to fail as it fails in the real cluster
/but as long as I cant get it to work with the array of points
  override val spec =
    """dtg:Date,
      |line:LineString:srid=4326
    """.stripMargin

  val sf = {
    val sf = new ScalaSimpleFeature(sft, "0")
    sf.setAttribute("dtg", "2012-01-01T19:00:00Z")
// for this Linestring heatmap fails in real cluster
    sf.setAttribute("line", "LINESTRING (121.78333282470703 31.133333206176758, 121.7763900756836 31.140277862548828, 121.76249694824219 31.153888702392578, 121.7558364868164 31.16083335876465, 121.741943359375 31.1744441986084, 121.73500061035156 31.18138885498047, 121.71416473388672 31.20194435119629, 121.67972564697266 31.23611068725586, 121.63833618164062 31.2772216796875, 121.58999633789062 31.325000762939453, 121.45166778564453 31.462223052978516, 121.3758316040039 31.537500381469727, 121.28583526611328 31.626388549804688, 121.18916320800781 31.721389770507812, 121.08555603027344 31.822500228881836, 120.96138763427734 31.943889617919922, 120.81610870361328 32.08583450317383, 120.73332977294922 32.16666793823242, 120.6066665649414 32.176944732666016, 120.36360931396484 32.1966667175293, 120.09972381591797 32.21833419799805, 119.81444549560547 32.24166488647461, 119.76166534423828 32.24583435058594, 119.64305877685547 32.44722366333008, 119.46277618408203 32.75361251831055, 119.44833374023438 32.77777862548828, 117.21305847167969 36.41777801513672, 116.81639099121094 37.223331451416016, 116.37860870361328 38.11305618286133, 116.42500305175781 38.372501373291016, 116.57166290283203 39.19499969482422, 116.85444641113281 39.79944610595703, 116.76083374023438 40.02305603027344, 116.74305725097656 40.06611251831055, 115.95194244384766 41.95861053466797, 115.26277923583984 42.23611068725586, 105.59610748291016 41.20500183105469, 104.47333526611328 41.030555725097656, 95.19944763183594 41.86166763305664, 86.71611022949219 43.5433349609375, 84.3647232055664 43.34583282470703, 80.7330551147461 43.50027847290039, 79.8066635131836 43.53305435180664, 79.5594482421875 43.540279388427734, 79.25027465820312 43.54916763305664, 78.0655517578125 43.57444381713867, 77.50861358642578 43.58222198486328, 77.15860748291016 43.59416580200195, 76.96083068847656 43.59611129760742, 76.80860900878906 43.59749984741211, 76.32611083984375 43.599998474121094, 75.53028106689453 43.635833740234375, 74.19889068603516 43.68305587768555, 73.74833679199219 43.696109771728516, 71.25444793701172 43.54666519165039, 70.86027526855469 43.51527786254883, 68.36666870117188 43.29999923706055, 67.79360961914062 43.19333267211914, 67.44721984863281 43.12722396850586, 66.77833557128906 42.99638748168945, 66.09249877929688 42.85749816894531, 64.2249984741211 42.45333480834961, 64 42.40277862548828, 63 42.1683349609375, 62.78666687011719 42.11722183227539, 62.630001068115234 42.079444885253906, 62.079166412353516 41.94388961791992, 60.654998779296875 41.578887939453125)")
//this works in the real cluster
//    sf.setAttribute("line", "LINESTRING (121.78333282470703 31.133333206176758, 121.7763900756836 31.140277862548828, 121.76249694824219 31.153888702392578, 121.7558364868164 31.16083335876465, 121.741943359375 31.1744441986084, 121.73500061035156 31.18138885498047 )")
    sf
  }

  def query = new Query(sftName, ECQL.toFilter("dtg DURING 2012-01-01T18:30:00.000Z/2012-01-01T19:30:00.000Z "))
*/


  "DensityProcess" should {
    val process = new DensityProcess()
 // this line below is not necessary when copying the method for the points used in StatsProcessTest.scala
 // addFeature(sf)
    "be able to compute heatmap for a Point " in {
      val radiusPixels = 10
      val envelope = new ReferencedEnvelope(-180, -90, 180, 90, CRS_EPSG_4326)
      val width = 480
      val height = 360
      val results = process.execute( fs.getFeatures(query),
        radiusPixels,
        null,
        null,
       envelope,
      width,
      height,
        null
      )
      ok
    }
  }
}