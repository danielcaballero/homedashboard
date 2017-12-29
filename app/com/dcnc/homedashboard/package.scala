package com.dcnc

package object homedashboard {
  final val DocumentDoesNotExistError = Symbol("DocumentDoesNotExistError")
  final val WriteIOError = Symbol("WriteIOError")
  final val JSONTransformError = Symbol("WriteIOError")
  final val DataCorruptionError = Symbol("DataCorruptionError")
  final val ValidationError = Symbol("ValidationError")
}
