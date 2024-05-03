package final_project

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx._
import org.apache.spark.storage.StorageLevel
import org.apache.log4j.{Level, Logger}
import scala.util.Random
import scala.collection.JavaConverters._
import org.apache.spark._
import org.apache.spark.broadcast.Broadcast

object matching {

def Greedy2(graph: Graph[Int, Int]): Graph[Int, Int] = {
  val sc = graph.edges.sparkContext
  //Initialize empty sets for edges and vertices
  val matchedVertices = sc.collectionAccumulator[Long]("Matched Vertices")
  val matchedEdges = sc.collectionAccumulator[Edge[Int]]("Matched Edges")
  
  //sort edges
  val edgesToConsider = graph.edges.sortBy(edge => (edge.srcId, edge.dstId))

  //partition the edges
  edgesToConsider.foreachPartition { iter =>
    val localMatchedVertices = scala.collection.mutable.Set[Long]()
    val localMatchedEdges = scala.collection.mutable.Set[Edge[Int]]()

    //iterate through the partitions and add new vertices/edges to matching
    iter.foreach { edge =>
      if (!localMatchedVertices.contains(edge.srcId) && !localMatchedVertices.contains(edge.dstId)) {
        localMatchedEdges.add(edge)
        localMatchedVertices.add(edge.srcId)
        localMatchedVertices.add(edge.dstId)
      }
    }
    
    //add vertices from partitions to original accumulators
    localMatchedVertices.foreach(matchedVertices.add)
    localMatchedEdges.foreach(matchedEdges.add)
  }

  //convert to set and broadcast
  val matchedVerticesList = matchedVertices.value.asScala.toSet
  val broadcastMatchedVertices = sc.broadcast(matchedVerticesList)

  var matchedEdgesList = matchedEdges.value.asScala.toArray.asInstanceOf[Array[Edge[Int]]]
  val finalEdgesList = scala.collection.mutable.ListBuffer[Edge[Int]]()

  // Set to keep track of seen vertices
  val seenVertices = scala.collection.mutable.Set[Long]()
  //removing overlap from partitions
  var i = 0
  while (i < matchedEdgesList.length) {
    val currentEdge = matchedEdgesList(i)
    if (!seenVertices.contains(currentEdge.srcId) && !seenVertices.contains(currentEdge.dstId)) {
      finalEdgesList += currentEdge
      seenVertices += currentEdge.srcId
      seenVertices += currentEdge.dstId
    }
    i += 1
  }
  
  val matchedEdgesRDD: RDD[Edge[Int]] = sc.parallelize(finalEdgesList)
  val matchedVerticesRDD: RDD[(Long, Int)] = graph.vertices.filter{ case (id, _) =>
    broadcastMatchedVertices.value.contains(id)
  }

  Graph(matchedVerticesRDD, matchedEdgesRDD)
 
}

    
 def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("matching")
    val sc = new SparkContext(conf)
    val spark = SparkSession.builder.config(conf).getOrCreate()
    
    val startTimeMillis = System.currentTimeMillis()
    val edges = sc.textFile(args(0)).map(line => {val x = line.split(","); Edge(x(0).toLong, x(1).toLong , 1)} )
    val g = Graph.fromEdges[Int, Int](edges, 0, edgeStorageLevel = StorageLevel.MEMORY_AND_DISK, vertexStorageLevel = StorageLevel.MEMORY_AND_DISK)
    val g2 = Greedy2(g)
    val endTimeMillis = System.currentTimeMillis()
    val durationSeconds = (endTimeMillis - startTimeMillis) / 1000
    val matchedDF = g2.edges.map(e => (e.srcId, e.dstId))
    val numMatchedEdges = matchedDF.count()
    println("====================================================================")
    println(s"Number of matched edges: $numMatchedEdges")
    println("====================================================================")
    println("====================================================================")
    println("Matching Discovered with Greedy in: " + durationSeconds + " seconds")
    println("====================================================================")
    val g2df = spark.createDataFrame(g2.edges)
    g2df.coalesce(1).write.format("csv").mode("overwrite").save(args(1))
  }
}