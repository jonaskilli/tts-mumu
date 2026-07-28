# Websocket

2025 版本后支持Websocket请求
, 其内部基于OkHttp实现

``` javascript
let ws = Websocket("wss://echo.websocket.org", {"User-Agent": "TTS Server"})
ws.on('close', function(code, reason){ // Int, String
    
})

ws.on('error', function(err, response){ // String, NativeResponse
    
})

ws.on('binary', function(buf){ // Buffer
    
})

ws.on('text', function(msg){ // String
    
})

ws.on('open', function(){
    ws.send("Hello world!")
})

// 关闭
// ws.close(1007, "close~~")

// 强制断开
// ws.cancel()

```

## 与插件TTS getAudioV2 配合

```javascript
var ws = null
let PluginJS = {
    // ...
    
    "onStop": function () {
        if (ws != null) {
            ws.cancel()
        }
    },

    "getAudioV2": function (request, callback) {
        // request 是为json类型
        // request.text /voice / locale / rate / volume / pitch
        // e.g. request.voice 
                
    
        callback.write(bytes)   // 写入字节数组
        callback.close()        // 全部写入完毕后调用
        callback.error(string)  // 发生错误调用
    }
    
}
// ...
    

```