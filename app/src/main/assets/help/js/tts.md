# TTS插件
> 插件的语言为Javascript，使用Rhino作为JS解析器

```javascript
let PluginJS = {
    "name": "插件名",
    "id": "唯一的插件ID", // 同时作为fs操作文件的相对目录 /Android/data/com.github.jing332.tts_server_android/caches/插件ID
    "author": "作者",
    "iconUrl": "https://cn.bing.com/favicon.ico",
    "version": 1, // 版本号，必须为整数

    // 当停止TTS时
    "onStop": function () {
 
    },
    
    /**
    "getAudio": function (text, locale, voice, speed, volume, pitch) {
        // 支持的返回类型：
        // http:// 和 https:// 开头的字符串
        // InputStream Java输入流
        // ByteArray Java字节数组
        // ArrayBuffer
        // Uint8Array
        
    }
    */

    // 与getAudio 根据需要二选一
    "getAudioV2": function (request, callback) {
        let rate = (request.rate * 2) - 100
        let pitch = request.pitch - 50
        let voice = request.voice
        let volume = request.volume
        let text = request.text

        callback.write(bytes)   // 写入字节数组
        callback.close()        // 全部写入完毕后调用
        callback.error(string)  // 发生错误调用
    },
}


let EditorJS = {
    // 音频的采样率，在保存TTS配置时调用
    "getAudioSampleRate": function (locale, voice) {
        // return 24000
        
        // 自动请求一段音频进行检测
        let audio = PluginJS.getAudio('test 测试', locale, voice, 50, 50, 50)
        return ttsrv.getAudioSampleRate(audio)
    },

    // 语言下拉框
    "getLocales": function () {
        return ['zh-CN', 'en-US']
    },

    // 音色下拉框
    "getVoices": function (locale) {
        // 简单用法， key作为getAudio的voice参数， value作为显示名
        return { 'xiaoxiao': '晓晓' }
        
        // 高级用法 指定图标
        return { 
            'xiaoxiao': {
                name: '晓晓',
                icon: 'male' // 可选值male / female / 图标url
            }
        }
      
    },

    // 加载语音数据
    "onLoadData": function () {
        let jsonStr = ''
        if (fs.exists('voices.json')) {
            jsonStr = fs.readText('voices.json')
        } else {
            let url = 'https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/voices/list?trustedclienttoken=' + token
            jsonStr = http.get(url).text()
            fs.writeFile('voices.json', jsonStr)
        }

        voices = JSON.parse(jsonStr)
    },

    "onLoadUI": function (ctx, linerLayout) {

    },

    "onVoiceChanged": function (locale, voiceCode) {

    }
}```

