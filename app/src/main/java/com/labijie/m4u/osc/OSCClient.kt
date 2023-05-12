package com.labijie.m4u.osc

import com.illposed.osc.OSCBundle
import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCSerializerAndParserBuilder
import com.illposed.osc.transport.OSCPortOut
import com.labijie.m4u.FaceSolver
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.net.PortUnreachableException

object OSCClient {
    private val logger = LoggerFactory.getLogger(OSCClient::class.java)
    private var oscPortOut : OSCPortOut? = null
    private var host: String = ""
    private var port: Int = 0
    private var hasConnError = false;

    val connected: Boolean
    get() { return oscPortOut?.isConnected == true && !hasConnError }



    fun connect(host:String, port: Int): Boolean {
        hasConnError = false
        if(oscPortOut == null || this.host != host || port != this.port) {
            if (oscPortOut?.isConnected == true) {
                try {
                    oscPortOut?.disconnect()
                } catch (_: IOException) {

                }
            }
            oscPortOut = null
            try {
                oscPortOut = OSCPortOut(
                    OSCSerializerAndParserBuilder(),
                    InetSocketAddress(host, port)
                )
                this.host = host
                this.port = port
            } catch (ex: Exception) {
                logger.error("Create network client fault.\nerror:${ex.message}",)
            }
        }
        if(oscPortOut != null && !oscPortOut!!.isConnected)
        {
            try {
                oscPortOut?.connect()
            }catch (ex: Exception)
            {
                logger.error("Connect remote host fault.\nerror:${ex.message}",)
            }
        }
        if(!connected)
        {
            logger.error("Unable to connect remote host (${host}:${port}).")
        }
        return connected
    }

    fun disconnect()
    {
        if (oscPortOut?.isConnected == true) {
            try {
                oscPortOut?.disconnect()
            } catch (_: IOException) {

            }
        }
    }

    fun send(result: FaceSolver.FaceSolverResult){
        if(!result.result.faceBlendshapes().isPresent)
        {
            return
        }
        if(oscPortOut?.isConnected != true)
        {
            oscPortOut?.disconnect()
            oscPortOut?.connect()
        }
        if (oscPortOut?.isConnected == true) {
            try {
                val bsMessage = OSCBundle()
                val list = result.result.faceBlendshapes().get().first().map { it.score() }
                bsMessage.addPacket(OSCMessage("/data/bs", list))
                oscPortOut?.send(bsMessage)
                hasConnError = false
            } catch (ex: IOException) {
                if(ex !is PortUnreachableException)
                {
                    logger.error("Send message to remote host fault.\nerror:${ex.message}")
                    hasConnError = true
                }
            }
        }
    }

    fun sendCommand(command: String): Boolean {
        if(oscPortOut?.isConnected != true)
        {
            oscPortOut?.disconnect()
            oscPortOut?.connect()
        }
        if (oscPortOut?.isConnected == true) {
            return try {
                val msg = OSCMessage("/cmd/$command")
                oscPortOut?.send(msg)
                hasConnError = false
                true
            } catch (ex: IOException) {
                if(ex !is PortUnreachableException) {
                    logger.error("Send message to remote host fault.\nerror:${ex.message}")
                    hasConnError = true
                }
                false
            }
        }
        return false
    }
}
