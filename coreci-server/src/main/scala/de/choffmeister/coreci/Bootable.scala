package de.choffmeister.coreci

import scala.reflect.Manifest

trait Bootable {
  def startup(args: List[String]): Unit
  def shutdown(): Unit
}

class BootableApp[T <: Bootable: Manifest] extends App {
  val manifest = implicitly[Manifest[T]]
  val bootable = manifest.runtimeClass.newInstance.asInstanceOf[T]
  sys.ShutdownHookThread(bootable.shutdown())
  bootable.startup(args.toList)
}
