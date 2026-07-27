package com.github.jing332.tts

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.synthesizer.SystemParams
import com.github.jing332.tts.speech.plugin.PluginTtsProvider
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TtsPluginEngineTest {
    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun ttsrv() {
        val code = """
            let token = ttsrv.userVars.token
            console.log(token)
            const PluginJS = {
                'name': 'lingban',
                'id': 'lingban.cn',
                'author': 'jing',
                'version': 2,
                'vars': {
                    token: { label: "Token", hint: "Your token" },
                },
                'onLoad': function() {
                    console.log("onLoad")
                    console.log(ttsrv.userVars)
                },
                
                // 无 getAudio() 时调用
                'getAudioV2': function(request, callback){
                    console.log(request)    
                },
                
                'getAudio': function(text, locale, voice, rate, volume, pitch) {
                    let reqUrl = 'https://paas.lingban.cn/tts/v1/audition'
                    let reqHeaders = {
                        'content-type': 'application/json;charset=UTF-8'
                    }
                    let reqBody = {
                        'text': text,
                        'speaker': voice,
                        'volume': volume,
                        'speed': rate,
                        'pitch': pitch
                    }
                    reqBody = JSON.stringify(reqBody)
                    
                    let resp = ttsrv.httpPost(reqUrl, reqBody, reqHeaders)
                    return resp.body().byteStream()
                }
            } 

            let EditorJS = {
                //在编辑TTS界面保存时调用
                'getAudioSampleRate': function(locale, voice) {
                    let audio = PluginJS.getAudio('test', locale, voice, 50, 50, 50)
                    let rate = ttsrv.getAudioSampleRate(audio)
            
                    return rate
                },
            
                'onLoadData': function() {
            
                },
            
                'getLocales': function() {
                    return ['zh-CN', 'en-US']
                },
            
                'getVoices': function(locale) {
                    console.log(typeof locale)
                    if (locale == 'zh-CN') {
                        return {'yanyan':'妍妍 [亲切甜美女声]','lulu':'璐璐 [轻快热情女声]','qiqi':'麒麒 [清新爽快女声]','nana':'娜娜 [轻柔恬淡女声]','xiaocheng':'晓诚 [似水如歌女声]','xiaoyao':'逍遥 [甜美灵动女声]','chenyu':'晨雨 [温和有礼女声]','lingfeng':'灵凤 [温婉动人女声]','ruirui':'瑞叡 [沉稳恬淡女声]','luyao':'露瑶 [婉转悠扬女声]','linglong':'玲珑 [轻快甜美女声]','yurou':'羽柔 [轻盈欢快女声]','siqi':'思绮 [宛转悠扬女声]','chenchen':'晨晨 [热情洋溢女声]','qiulian':'秋莲 [春风舒畅女声]','xintong':'馨彤 [清澈动听女声]','yiyi':'伊依 [清朗悦耳女声]','wenhui':'文慧 [珠圆玉润女声]','xiaoxiao':'筱筱 [可信亲切女声]','xiaofang':'小芳 [天真活泼女声]','xiaoai':'小艾 [真诚友善女声]','nainai':'奈奈 [纯真自然女声]','chengmiao':'承渺 [轻劲飒爽女声]','zhengcheng':'征程 [柔和坚定男声]','zili':'子黎 [清幽雅致男声]','ningning':'宁宁 [亲切温柔男声]','zhentian':'振天 [大方洒脱男声]','bufan':'步凡 [磁性淳厚男声]','wenxi':'文熙 [阳光自然男声]','tiantian':'天天 [沉稳浑厚男声]','lingyu':'凌宇 [温润如玉男声]','wusong':'梧松 [明朗严肃男声]','kunde':'坤德 [深沉稳重男声]','jinyan':'锦言 [清晰严谨男声]','shenzhi':'深秩 [紧张压迫男声]','ruyan':'儒严 [一本正经男声]','jianming':'剑鸣 [玄幻小说]','yuzhang':'御彰 [玄幻小说]','chuanqi':'传奇 [游戏小说]','lingmeng':'绫梦 [言情小说]','shenghun':'圣魂 [玄幻小说]','yizhou':'易周 [都市小说]','youlan':'幽蓝 [言情小说]','hongmang':'鸿芒 [玄幻小说]','youran':'悠然 [玄幻小说]','shanyu':'山雨 [玄幻小说]','mengjing':'梦静 [都市小说]','yunsong':'云颂 [玄幻小说]','haohan':'浩翰 [都市小说]','linger':'灵儿 [萝莉音色]','yaoyao':'妖妖 [正太音色]','subo':'苏博 [新闻男声]','mina':'米娜 [新闻女声]','linghui':'灵慧 [新闻女声]','lingyin':'灵隐 [新闻女声]','shuangshuang':'双双 [新闻男声]','jeff':'杰夫 [英文新闻]','rose':'萝丝 [英文小说]','julia':'茱莉亚 [英文小说]','jack':'杰克 [英文小说]','liangshu':'梁叔 [名人音色]','xiaosong':'小松 [名人音色]','laoyi':'老易 [名人音色]','dafei':'大飞 [名人音色]','shishen':'食神 [名人音色]','wansheng':'万圣 [名人音色]','kaige':'凯哥 [名人音色]','guobao':'国宝 [名人音色]','jiange':'健哥 [名人音色]'}
                    }
                },
            
                'onLoadUI': function(ctx, linearLayout) {
            
                }
            }
        """.trimIndent()

        val plugin = Plugin(code = code, userVars = mapOf("token" to "value-token"))
        val tte = TtsPluginUiEngineV2(context, plugin)
 //        tte.onLoadData()
//        val locales = tte.getLocales()
        val voices = tte.getVoices("zh-CN")


//        tte.getAudio(
//            text = "君不见黄河之水天上来，奔流到海不复回。",
//            locale = "zh-CN",
//            voice = "test-li-bai",
//            rate = 1.2f,
//            volume = 1.25f,
//            pitch = 1.80f
//        )

    }


    @Test
    fun cancelable() = runBlocking {
        val code = """
        const PluginJS = {
            'name': 'lingban',
            'id': 'lingban.cn',
            'author': 'jing',
            'version': 2,
            'vars': {
                bearer: { label: "token", hint: "Your token" },
            },
            'onLoad': function() {
                console.log("onLoad")
            },
            
            // 无 getAudio() 时调用
            'getAudioV2': function(request, callback){
                console.log(request)    
            },
            
            'getAudio': function(text, locale, voice, rate, volume, pitch) {
                let reqUrl = 'https://paas.lingban.cn/tts/v1/audition'
                let reqHeaders = {
                    'content-type': 'application/json;charset=UTF-8'
                }
                let reqBody = {
                    'text': text,
                    'speaker': voice,
                    'volume': volume,
                    'speed': rate,
                    'pitch': pitch
                }
                reqBody = JSON.stringify(reqBody)
                console.log(reqBody)

                java.lang.Thread.sleep(5000)
                let resp = ttsrv.httpPost(reqUrl, reqBody, reqHeaders)
                return resp.body().byteStream()
            }
            
       } 
        """.trimIndent()
        val ttsEngineService = object : PluginTtsProvider(context, Plugin(code = code)) {
        }
        val job = launch(Dispatchers.IO) {
            ttsEngineService.onInit()
            val inputStream = ttsEngineService.getStream(
                SystemParams(text = "Hello, I am androidTest"),
                PluginTtsSource(pluginId = "", locale = "zh-CN", voice = "")
            )
        }
        delay(1000)
        job.cancel()
    }

    @Test
    fun testUiJsImporter() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val code = """
            LinearLayout.VERTICAL
        """.trimIndent()
        val engine = TtsPluginUiEngineV2(context, Plugin(code = code))

     }

}