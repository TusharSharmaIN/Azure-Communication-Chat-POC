package com.android.azure_communication_chat_poc

/*fun main() {
    // Some faulty data with ages of our users
    val data = mapOf(
        "users1.csv" to listOf(32, 45, 17, -1, 34, -2),
        "users2.csv" to listOf(19, -1, 67, 22),
        "users3.csv" to listOf(),
        "users4.csv" to listOf(56, 32, 18, 44)
    )

    val listOfAges = mutableListOf<Int>()
    data.values.forEach { ageList ->
        ageList.forEach {age ->
            if(age > 0) {
                listOfAges.add(age)
            }
        }
    }

    println(listOfAges.average())



    data.forEach { map ->
        val filteredList = map.value.filterIndexed { index, age ->
            age < 0
        }.isNotEmpty()
        if (filteredList) {
            println(map.key)
        }
    }

    var faultyValues = 0
    data.forEach {map ->
        val filteredList = map.value.filterIndexed { index, age ->
            age < 0
        }
        faultyValues += filteredList.count()
    }
    println(faultyValues)



    *//*Better Approaches*//*

    val cleanedData = data.flatMap { it.value }
        .filter { it in 0..100 }
        .toList()

    println(cleanedData.average())


    val possiblePrimesAfter2 = generateSequence(3) {it + 2}

    val primes = generateSequence(2 to possiblePrimesAfter2) { it ->
        val nextPrimeNumber = it.second.first()
        println(nextPrimeNumber)

        val possiblePrimesAfterNextPrimeNumber = it.second.filter { it % nextPrimeNumber != 0 }

        println("nextPrimeNumber->$nextPrimeNumber, possiblePN-> ${possiblePrimesAfterNextPrimeNumber.toString()}")

        nextPrimeNumber to possiblePrimesAfterNextPrimeNumber
    }.map { it.first }

    println("primes-> ${primes.take(10).toList()}")

}*/


interface Drivable {
    fun drive()

    fun drive2() {
        println("Drivable Drive 2.....")
    }
}

interface Drivable2 {
    fun drive()
    fun drive2() {
        println("Drivable2 Drive 2.....")
    }
}

class Bicycle: Drivable, Drivable2 {
    fun myFun() = println("Kavita")

    override fun drive() {
        println("Driving Bicycle")
    }

    override fun drive2() {
        super<Drivable>.drive2()
    }
}

class Car: Drivable {
    override fun drive() {
        println("Driving Car")
    }
}

fun main() {
    val bicycleObj: Bicycle = Bicycle()
    bicycleObj.drive()
    bicycleObj.drive2()
    bicycleObj.myFun()
}