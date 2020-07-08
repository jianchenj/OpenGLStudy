package com.jchen.avedit


data class Photo(override var name: String, override var date: String, override var size: Long, override var path: String) : AvItem