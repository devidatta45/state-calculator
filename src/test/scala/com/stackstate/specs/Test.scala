package com.stackstate.specs

import scala.annotation.tailrec

object Test extends App {

  val priceMap: Map[String, Int] = Map("1$" -> 100, "50c" -> 50, "25c" -> 25, "10c" -> 10, "5c" -> 5, "1c" -> 1)

  def getChange(input: Double, priceItem: Double): Map[String, Int] = {
    val change = input * 100  - priceItem * 100
    changeRecursively(priceMap.toVector.sortBy(_._2).reverse, change.toInt)

  }

  @tailrec
  private def changeRecursively(map: Vector[(String, Int)], finalAmount: Int, accumulator: Map[String, Int] = Map.empty ): Map[String, Int] = {
    if(finalAmount == 0 || map.isEmpty) {
      accumulator
    } else {
      val (changeValue, priceValue) = map.head
      if(finalAmount >= priceValue){
        val occurs = accumulator.getOrElse(changeValue, 0)
        changeRecursively(map, finalAmount - priceValue, accumulator.updated(changeValue, occurs + 1))
      } else {
        changeRecursively(map.tail, finalAmount, accumulator)
      }
    }
  }

  println(getChange(5, 0.99))

}
