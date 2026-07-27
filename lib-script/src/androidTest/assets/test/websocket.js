let latch = new java.util.concurrent.CountDownLatch(1);

let ws = new Websocket("wss://echo.websocket.org", {"User-Agent": "TTS Server"})
ws.on('close', function(code, reason){ // Int, String
   println("closed: " + code + ", " + reason)
   latch.countDown()
})

ws.on('error', function(err, response){ // String, NativeResponse
    println("error: " + err + ", code: " + response.status + ", message: " + response.statusText)
    latch.countDown()
})

ws.on('binary', function(buf){ // Buffer
    println("receive binary: " + buf)
    ws.close(1007, "close~~")
})

ws.on('text', function(msg){ // String
    println("receive text: " + msg)
})

ws.on('open', function(){
    ws.send("Hello world!")

    let buf = Buffer.from("Hello world!")
    ws.send(buf)
})

latch.await()

// 强制断开
// ws.cancel()