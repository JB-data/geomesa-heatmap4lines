/***********************************************************************
 * Copyright (c) 2013-2022 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.hbase.data

import com.typesafe.scalalogging.LazyLogging
import org.geotools.data.{Query, _}
import org.geotools.filter.text.ecql.ECQL
import org.geotools.geometry.jts.ReferencedEnvelope
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.geotools.util.factory.Hints
import org.junit.runner.RunWith
import org.locationtech.geomesa.features.ScalaSimpleFeature
import org.locationtech.geomesa.filter.FilterHelper
import org.locationtech.geomesa.index.conf.QueryHints
import org.locationtech.geomesa.index.iterators.DensityScan
import org.locationtech.geomesa.utils.collection.SelfClosingIterator
import org.locationtech.geomesa.utils.geotools.{FeatureUtils, SimpleFeatureTypes}
import org.locationtech.geomesa.utils.io.WithClose
import org.locationtech.jts.geom.Envelope
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.opengis.filter.Filter
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.Date
import scala.util.Random

//JBaddedimportstart
import org.locationtech.geomesa.process.analytic.DensityProcess
import org.geotools.geometry.jts.ReferencedEnvelope
import org.locationtech.geomesa.utils.geotools.{CRS_EPSG_4326, FeatureUtils}
//JBaddedimportstop


@RunWith(classOf[JUnitRunner])
class HBaseDensityProcessTest extends Specification with LazyLogging {

  import scala.collection.JavaConverters._

  sequential


  val TEST_FAMILY = "an_id:java.lang.Integer,attr:java.lang.Double,dtg:Date,geom:Point:srid=4326"
  val TEST_HINT = new Hints()
  val typeName = "HBaseDensityFilterTest"

  lazy val params = Map(
    HBaseDataStoreParams.ConnectionParam.getName     -> MiniCluster.connection,
    HBaseDataStoreParams.HBaseCatalogParam.getName   -> getClass.getSimpleName,
    HBaseDataStoreParams.DensityCoprocessorParam.key -> true
  )

  lazy val ds = DataStoreFinder.getDataStore(params.asJava).asInstanceOf[HBaseDataStore]
  lazy val dsSemiLocal = DataStoreFinder.getDataStore((params ++ Map(HBaseDataStoreParams.DensityCoprocessorParam.key -> false)).asJava).asInstanceOf[HBaseDataStore]
  lazy val dsFullLocal = DataStoreFinder.getDataStore((params ++ Map(HBaseDataStoreParams.RemoteFilteringParam.key -> false)).asJava).asInstanceOf[HBaseDataStore]
  lazy val dsThreads1 = DataStoreFinder.getDataStore((params ++ Map(HBaseDataStoreParams.CoprocessorThreadsParam.key -> "1")).asJava).asInstanceOf[HBaseDataStore]
  lazy val dsYieldPartials = DataStoreFinder.getDataStore((params ++ Map(HBaseDataStoreParams.YieldPartialResultsParam.key -> true)).asJava).asInstanceOf[HBaseDataStore]
  lazy val dataStores = Seq(ds, dsSemiLocal, dsFullLocal, dsThreads1, dsYieldPartials)

  var sft: SimpleFeatureType = _

  step {
    logger.info("Starting the Density Filter Test")
    ds.getSchema(typeName) must beNull
    ds.createSchema(SimpleFeatureTypes.createType(typeName, TEST_FAMILY))
    sft = ds.getSchema(typeName)
  }




  //JBSTART LINESTRING TEST

  val defaultFilter = ECQL.toFilter("bbox(geom,-180,-90,180,90) and dtg DURING 2012-01-01T18:30:00.000Z/2012-01-01T19:30:00.000Z")
  val spec =
    """dtg:Date,
      |line:LineString:srid=4326
    """.stripMargin

  val sf = {
    val sf = new ScalaSimpleFeature(sft, "0")
    sf.setAttribute("dtg", "2012-01-01T19:00:00Z")
    // for this Linestring heatmap fails in real cluster
    sf.setAttribute("line", "LINESTRING (121.78333282470703 31.133333206176758, 121.7763900756836 31.140277862548828, 121.76249694824219 31.153888702392578, 121.7558364868164 31.16083335876465, 121.741943359375 31.1744441986084, 121.73500061035156 31.18138885498047, 121.71416473388672 31.20194435119629, 121.67972564697266 31.23611068725586, 121.63833618164062 31.2772216796875, 121.58999633789062 31.325000762939453, 121.45166778564453 31.462223052978516, 121.3758316040039 31.537500381469727, 121.28583526611328 31.626388549804688, 121.18916320800781 31.721389770507812, 121.08555603027344 31.822500228881836, 120.96138763427734 31.943889617919922, 120.81610870361328 32.08583450317383, 120.73332977294922 32.16666793823242, 120.6066665649414 32.176944732666016, 120.36360931396484 32.1966667175293, 120.09972381591797 32.21833419799805, 119.81444549560547 32.24166488647461, 119.76166534423828 32.24583435058594, 119.64305877685547 32.44722366333008, 119.46277618408203 32.75361251831055, 119.44833374023438 32.77777862548828, 117.21305847167969 36.41777801513672, 116.81639099121094 37.223331451416016, 116.37860870361328 38.11305618286133, 116.42500305175781 38.372501373291016, 116.57166290283203 39.19499969482422, 116.85444641113281 39.79944610595703, 116.76083374023438 40.02305603027344, 116.74305725097656 40.06611251831055, 115.95194244384766 41.95861053466797, 115.26277923583984 42.23611068725586, 105.59610748291016 41.20500183105469, 104.47333526611328 41.030555725097656, 95.19944763183594 41.86166763305664, 86.71611022949219 43.5433349609375, 84.3647232055664 43.34583282470703, 80.7330551147461 43.50027847290039, 79.8066635131836 43.53305435180664, 79.5594482421875 43.540279388427734, 79.25027465820312 43.54916763305664, 78.0655517578125 43.57444381713867, 77.50861358642578 43.58222198486328, 77.15860748291016 43.59416580200195, 76.96083068847656 43.59611129760742, 76.80860900878906 43.59749984741211, 76.32611083984375 43.599998474121094, 75.53028106689453 43.635833740234375, 74.19889068603516 43.68305587768555, 73.74833679199219 43.696109771728516, 71.25444793701172 43.54666519165039, 70.86027526855469 43.51527786254883, 68.36666870117188 43.29999923706055, 67.79360961914062 43.19333267211914, 67.44721984863281 43.12722396850586, 66.77833557128906 42.99638748168945, 66.09249877929688 42.85749816894531, 64.2249984741211 42.45333480834961, 64 42.40277862548828, 63 42.1683349609375, 62.78666687011719 42.11722183227539, 62.630001068115234 42.079444885253906, 62.079166412353516 41.94388961791992, 61.75 41.86166763305664, 60.654998779296875 41.578887939453125, 60.1974983215332 41.4716682434082, 59.66749954223633 41.34333419799805, 56.26444625854492 41.15638732910156, 51.5 40.724998474121094, 50.43555450439453 40.678890228271484, 49.343055725097656 40.8849983215332, 47.98027801513672 41.10916519165039, 46.75 41.29666519165039, 45.36666488647461 41.483333587646484, 44.231388092041016 41.57194519042969, 43.49583435058594 41.627777099609375, 42.85972213745117 41.67250061035156, 41.44972229003906 41.54888916015625, 40.633609771728516 41.59388732910156, 39.58305740356445 41.643333435058594, 37.66194534301758 41.70861053466797, 34.594722747802734 41.74611282348633, 33.810001373291016 41.9275016784668, 33.706111907958984 41.95138931274414, 33.07777786254883 42.071388244628906, 32.61194610595703 42.16611099243164, 32.20500183105469 42.246944427490234, 32.1238899230957 42.262779235839844, 31.107500076293945 42.45722198486328, 31.00055503845215 42.47666549682617, 30.26444435119629 42.61055374145508, 29.143333435058594 43.733333587646484, 22.108055114746094 47.573612213134766, 21.29138946533203 48.52305603027344, 21.108333587646484 49.36916732788086, 20.693889617919922 51.28444290161133, 20.721111297607422 51.391387939453125, 21.02166748046875 52.56694412231445, 21.738889694213867 53.15666580200195, 22.900278091430664 54.06972122192383, 23.059165954589844 54.307498931884766, 23.068889617919922 54.36138916015625, 23.426389694213867 56.36305618286133, 23.455554962158203 56.41444396972656, 24.2994441986084 57.900001525878906, 24.509721755981445 58.50138854980469, 24.52833366394043 58.554443359375, 24.586944580078125 58.72249984741211, 24.59666633605957 58.74972152709961, 24.661945343017578 58.93111038208008, 24.6875 59.000831604003906, 24.758054733276367 59.19305419921875, 24.838333129882812 59.4113883972168, 24.934722900390625 59.52777862548828, 25.053611755371094 59.6694450378418, 25.14472198486328 59.77777862548828, 25.18666648864746 59.82777786254883, 25.251667022705078 59.90416717529297, 25.182222366333008 60.01333236694336, 25.150278091430664 60.0636100769043, 25.099721908569336 60.14277648925781, 25.04888916015625 60.154998779296875, 24.913055419921875 60.18805694580078, 24.79861068725586 60.135555267333984, 24.89583396911621 60.24277877807617, 24.963333129882812 60.317222595214844)")
    sf
  }
  lazy val sftName = sft.getTypeName
  def query = new Query(sftName, ECQL.toFilter("dtg DURING 2012-01-01T18:30:00.000Z/2012-01-01T19:30:00.000Z "))


  "HbaseDensityProcess" should {
    val process = new DensityProcess()
    addFeature(sf)

    "be able to compute heatmap for a LINE " in {
      val envelope = new ReferencedEnvelope(-180, -90, 180, 90, CRS_EPSG_4326)
      val width = 768
      val height = 384
      val query = process.invertQuery(
        null,
        null,
        null,
        envelope,
        width,
        height,
        new Query(sftName, defaultFilter),
        null
      )
      lazy val fs = ds.getFeatureSource(sftName)
      val results = process.execute( fs.getFeatures(query),
        null,
        null,
        null,
        envelope,
        width,
        height,
        null
      )
      results must not(beNull)
    }
  }
  def addFeature(feature: SimpleFeature): Unit = {
    WithClose(ds.getFeatureWriterAppend(typeName, Transaction.AUTO_COMMIT)) { writer =>
      FeatureUtils.write(writer, feature, useProvidedFid = true)
    }
  }

  //JBEND LINESTRING TEST


  step {
    logger.info("Cleaning up HBase Density Test")
    dataStores.foreach { _.dispose() }
  }

  def addFeatures(features: Seq[SimpleFeature]): Unit = {
    WithClose(ds.getFeatureWriterAppend(typeName, Transaction.AUTO_COMMIT)) { writer =>
      features.foreach(FeatureUtils.write(writer, _, useProvidedFid = true))
    }
  }



  def clearFeatures(): Unit = {
    val writer = ds.getFeatureWriter(typeName, Filter.INCLUDE, Transaction.AUTO_COMMIT)
    while (writer.hasNext) {
      writer.next()
      writer.remove()
    }
    writer.close()
  }

  def getDensity(typeName: String, query: String, ds: DataStore): List[(Double, Double, Double)] = {
    val filter = ECQL.toFilter(query)
    val envelope = FilterHelper.extractGeometries(filter, "geom").values.headOption match {
      case None    => ReferencedEnvelope.create(new Envelope(-180, 180, -90, 90), DefaultGeographicCRS.WGS84)
      case Some(g) => ReferencedEnvelope.create(g.getEnvelopeInternal,  DefaultGeographicCRS.WGS84)
    }
    val q = new Query(typeName, filter)
    q.getHints.put(QueryHints.DENSITY_BBOX, envelope)
    q.getHints.put(QueryHints.DENSITY_WIDTH, 500)
    q.getHints.put(QueryHints.DENSITY_HEIGHT, 500)
    val decode = DensityScan.decodeResult(envelope, 500, 500)
    SelfClosingIterator(ds.getFeatureReader(q, Transaction.AUTO_COMMIT)).flatMap(decode).toList
  }


}
