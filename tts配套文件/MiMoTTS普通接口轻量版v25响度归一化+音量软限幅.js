var PluginJS = {
    'name': "MiMoTTS",
    'id': "xiaomi.mimo.tts.normaldesign.api.v24",
    'author': "ChatGPT ",
    'description': '普通接口轻量版：只包含普通内置音色、情绪/方言/唱歌标签、VoiceDesign；不包含 VoiceClone 和 URL 下载；v25 在 v24(尾部静音补偿+结尾停顿保护)基础上增加响度归一化(峰值拉满)与音量软限幅，解决返回音频母带偏轻导致音量小的问题。',
    'version': 25,
    'BASE_URL': 'https://api.xiaomimimo.com/v1',
    'vars': {
        'api_key': {
            'name': '普通接口 API密钥',
            'hint': '请输入 sk- 开头的 MiMo 普通接口 API Key',
            'binding': 'user_input'
        }
    },
    'VOICES': {
        'builtin|mimo_default': 'MiMo Default｜默认 [V2.5]',
        'builtin|冰糖': '冰糖 [V2.5]',
        'builtin|茉莉': '茉莉 [V2.5]',
        'builtin|苏打': '苏打 [V2.5]',
        'builtin|白桦': '白桦 [V2.5]',
        'builtin|Mia': 'Mia [V2.5]',
        'builtin|Chloe': 'Chloe [V2.5]',
        'builtin|Milo': 'Milo [V2.5]',
        'builtin|Dean': 'Dean [V2.5]',
        'builtin|default_zh': '中文女声 [V2]',
        'builtin|default_en': '英文女声 [V2]',

        'tag|开心|mimo_default': '情绪｜开心｜默认',
        'tag|悲伤|mimo_default': '情绪｜悲伤｜默认',
        'tag|生气|mimo_default': '情绪｜生气｜默认',
        'tag|温柔|mimo_default': '情绪｜温柔｜默认',
        'tag|惊讶|mimo_default': '情绪｜惊讶｜默认',
        'tag|冷静|mimo_default': '情绪｜冷静｜默认',
        'tag|粤语|mimo_default': '方言｜粤语｜默认',
        'tag|四川话|mimo_default': '方言｜四川话｜默认',
        'tag|东北话|mimo_default': '方言｜东北话｜默认',
        'tag|台湾普通话|mimo_default': '方言｜台湾普通话｜默认',
        'tag|唱歌|mimo_default': '唱歌｜默认',

        'design|deep_radio': 'VoiceDesign｜深夜电台男声',
        'design|warm_sister': 'VoiceDesign｜温柔姐姐',
        'design|cold_narrator': 'VoiceDesign｜冷静旁白',
        'design|cute_girl': 'VoiceDesign｜元气少女',
        'design|elder_story': 'VoiceDesign｜长者故事感',
        'design|custom': 'VoiceDesign｜自定义描述'
    },
    'DESIGN_PROMPTS': {
        'design|deep_radio': '一位语速舒缓、声音醇厚、适合深夜电台的中年男主播。',
        'design|warm_sister': '一位声音温柔、亲切自然、语气带安抚感的年轻女性。',
        'design|cold_narrator': '一位冷静、清晰、专业的纪录片旁白，语速中等，咬字准确。',
        'design|cute_girl': '一位活泼开朗、元气充足、语调上扬的年轻女孩。',
        'design|elder_story': '一位沉稳慈祥、讲故事感强、语速偏慢的长者声音。',
        'design|custom': ''
    },
    'STYLE_TAGS': ['开心', '悲伤', '生气', '惊讶', '恐惧', '厌恶', '抒情', '平淡', '温柔', '冷静', '粤语', '四川话', '东北话', '陕西话', '天津话', '上海话', '山东话', '河南话', '台湾普通话', '唱歌'],

    'getAudio': function(text, locale, voice, speed, volume, pitch) {
        var apiKey = String(ttsrv.userVars['api_key'] || '');
        if (!apiKey) {
            throw new Error('请先在插件列表右侧三个点 → 设置变量 中填写普通接口 API密钥。');
        }
        if (apiKey.indexOf('tp-') === 0) {
            throw new Error('当前是普通接口版，请填写 sk- 开头的 API Key；tp- Key 请用专属接口版。');
        }

        var voiceId = String(voice || 'builtin|mimo_default');
        var parts = voiceId.split('|');
        var kind = parts[0] || 'builtin';
        var model = 'mimo-v2.5-tts';
        var content = this._cleanText(String(text || ''));
        var messages = [];
        var audioFormat = String(ttsrv.tts.data['audio_format'] || 'wav').toLowerCase();
        // 尾部补静音只对 WAV 最可靠；这里固定请求 WAV，避免播放器在 MP3/Ogg 末尾截断。
        if (audioFormat !== 'wav') audioFormat = 'wav';
        var audioConfig = { 'format': audioFormat };

        if (kind === 'design') {
            model = 'mimo-v2.5-tts-voicedesign';
            var customPrompt = this._trim(ttsrv.tts.data['voice_design_prompt']);
            var prompt = customPrompt || this.DESIGN_PROMPTS[voiceId] || this.DESIGN_PROMPTS['design|deep_radio'];
            if (!prompt) {
                prompt = '请生成一个自然、清晰、适合有声书朗读的声音。';
            }
            messages.push({ 'role': 'user', 'content': prompt });
        } else {
            var targetVoice = parts[1] || 'mimo_default';
            var styleTag = this._trim(ttsrv.tts.data['style_tag']);
            if (kind === 'tag') {
                targetVoice = parts[2] || 'mimo_default';
                styleTag = parts[1] + (styleTag ? (' ' + styleTag) : '');
            }
            audioConfig['voice'] = targetVoice;
            var userPrompt = this._trim(ttsrv.tts.data['user_prompt']);
            if (userPrompt) {
                messages.push({ 'role': 'user', 'content': userPrompt });
            }
            if (styleTag) {
                content = '<style>' + styleTag + '</style>' + content;
            }
        }

        content = this._addEndGuard(content);
        messages.push({ 'role': 'assistant', 'content': content });

        var requestBody = {
            'model': model,
            'messages': messages,
            'audio': audioConfig
        };

        return this._postTTS(requestBody, apiKey, volume);
    },

    '_postTTS': function(requestBody, apiKey, volume) {
        var url = this.BASE_URL + '/chat/completions';
        var response = ttsrv.httpPost(url, JSON.stringify(requestBody), {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + apiKey
        });
        var body = response.body().string();
        if (response.code() !== 200) {
            throw new Error('[MiMoTTS 普通接口 普通+VoiceDesign] API请求失败: HTTP ' + response.code() + ' ' + this._extractError(body));
        }
        var json = JSON.parse(body);
        var audioBase64 = null;
        if (json && json.choices && json.choices.length > 0 && json.choices[0].message && json.choices[0].message.audio) {
            audioBase64 = json.choices[0].message.audio.data;
        }
        if (!audioBase64) {
            throw new Error('[MiMoTTS 普通接口 普通+VoiceDesign] 响应中未找到 choices[0].message.audio.data。原始响应: ' + body.substring(0, 300));
        }
        var bytes = java.util.Base64.getDecoder().decode(audioBase64);
        var playable = this._ensurePlayableAudio(bytes);
        // v25：响度归一化 + 叠加音量(软限幅)，解决母带偏轻音量小
        playable = this._applyGain(playable, volume);
        return this._appendTailSilence(playable, this._getTailSilenceMs());
    },

    '_getTailSilenceMs': function() {
        var raw = parseInt(String(ttsrv.tts.data['tail_silence_ms'] || '900'), 10);
        if (isNaN(raw)) raw = 900;
        if (raw < 0) raw = 0;
        if (raw > 3000) raw = 3000;
        return raw;
    },

    '_addEndGuard': function(s) {
        var text = String(s || '');
        if (!text) return text;
        var guard = this._trim(ttsrv.tts.data['end_guard_text']);
        if (!guard) return text;
        // 只允许短停顿符，避免误把大段额外文字读出来。
        if (guard.length > 12) guard = guard.substring(0, 12);
        return text + guard;
    },

    '_appendTailSilence': function(bytes, ms) {
        if (!bytes || !ms || ms <= 0) return bytes;
        if (!this._hasMagic(bytes, 0x52, 0x49, 0x46, 0x46)) return bytes; // 只处理 RIFF/WAV
        if (bytes.length < 44) return bytes;

        var info = this._findWavDataChunk(bytes);
        if (!info) return bytes;

        var sampleRate = this._readInt32LE(bytes, 24);
        var blockAlign = this._readInt16LE(bytes, 32);
        if (!sampleRate || sampleRate < 8000 || sampleRate > 192000) sampleRate = 24000;
        if (!blockAlign || blockAlign < 1 || blockAlign > 32) {
            var channels = this._readInt16LE(bytes, 22) || 1;
            var bits = this._readInt16LE(bytes, 34) || 16;
            blockAlign = Math.max(1, Math.floor(channels * Math.max(1, bits / 8)));
        }

        var tailBytes = Math.floor(sampleRate * ms / 1000) * blockAlign;
        if (tailBytes <= 0) return bytes;

        var dataEnd = info.dataOffset + info.dataSize;
        if (dataEnd > bytes.length) return bytes;

        var baos = new java.io.ByteArrayOutputStream(bytes.length + tailBytes);
        baos.write(bytes, 0, dataEnd);
        for (var i = 0; i < tailBytes; i++) baos.write(0);
        if (dataEnd < bytes.length) baos.write(bytes, dataEnd, bytes.length - dataEnd);
        var out = baos.toByteArray();
        baos.close();

        this._patchInt32LE(out, 4, out.length - 8);
        this._patchInt32LE(out, info.sizeOffset, info.dataSize + tailBytes);
        return out;
    },

    '_s16': function(bytes, offset) {
        var v = this._readInt16LE(bytes, offset);
        return v >= 32768 ? v - 65536 : v;
    },

    // 响度归一化 + 音量叠加：MiMo 普通接口(chat/completions)返回的 WAV 通常母带偏轻，
    // 先按峰值归一化到约 90% 满量程，再把传入音量作为额外增益(带 tanh 软限幅防硬削波)。
    // 仅处理 WAV，其余格式原样返回。
    '_applyGain': function(bytes, volume) {
        if (!bytes || bytes.length < 44) return bytes;
        if (!this._hasMagic(bytes, 0x52, 0x49, 0x46, 0x46)) return bytes; // 仅处理 RIFF/WAV
        var info = this._findWavDataChunk(bytes);
        if (!info) return bytes;
        var n = info.dataSize, off = info.dataOffset, i, peak = 0;
        for (i = 0; i + 1 < n && off + i + 1 < bytes.length; i += 2) {
            var a = this._s16(bytes, off + i);
            if (a < 0) a = -a;
            if (a > peak) peak = a;
        }
        if (peak < 16) return bytes;              // 近乎静音/无效数据，不动
        var normGain = 0.90 * 32767 / peak;        // 归一化到约 -0.9 dBFS(留余量防爆音)
        var vol = (typeof volume === 'number' && volume > 0) ? volume : 1;
        vol = Math.max(1, Math.min(vol, 4));       // 额外增益上限，配合软限幅

        var gain = normGain * vol;
        var baos = new java.io.ByteArrayOutputStream(bytes.length);
        baos.write(bytes, 0, off);                 // 先拷贝 WAV 头
        for (i = 0; i + 1 < n && off + i + 1 < bytes.length; i += 2) {
            // tanh 软限幅：小信号近似线性放大，大信号平滑压缩，避免硬削波
            var y = Math.tanh(this._s16(bytes, off + i) / 32767.0 * gain) * 32767.0;
            var v = Math.round(y);
            baos.write(v & 0xFF);
            baos.write((v >> 8) & 0xFF);
        }
        if (off + i < bytes.length) baos.write(bytes, off + i, bytes.length - off - i);
        var out = baos.toByteArray();
        baos.close();
        return out;
    },

    '_findWavDataChunk': function(bytes) {
        if (!bytes || bytes.length < 44) return null;
        if (!this._hasMagic(bytes, 0x52, 0x49, 0x46, 0x46)) return null; // RIFF
        if (this._chunkId(bytes, 8) !== 'WAVE') return null;

        var pos = 12;
        while (pos + 8 <= bytes.length) {
            var id = this._chunkId(bytes, pos);
            var size = this._readInt32LE(bytes, pos + 4);
            if (size < 0) return null;
            var dataOffset = pos + 8;
            var dataEnd = dataOffset + size;
            if (dataOffset > bytes.length || dataEnd > bytes.length) return null;
            if (id === 'data') {
                return { 'sizeOffset': pos + 4, 'dataOffset': dataOffset, 'dataSize': size };
            }
            pos = dataEnd + (size % 2);
        }
        return null;
    },

    '_chunkId': function(bytes, offset) {
        if (!bytes || offset + 3 >= bytes.length) return '';
        return String.fromCharCode(this._u8(bytes[offset])) +
               String.fromCharCode(this._u8(bytes[offset + 1])) +
               String.fromCharCode(this._u8(bytes[offset + 2])) +
               String.fromCharCode(this._u8(bytes[offset + 3]));
    },

    '_readInt16LE': function(bytes, offset) {
        if (!bytes || offset + 1 >= bytes.length) return 0;
        return this._u8(bytes[offset]) + this._u8(bytes[offset + 1]) * 256;
    },

    '_readInt32LE': function(bytes, offset) {
        if (!bytes || offset + 3 >= bytes.length) return 0;
        return this._u8(bytes[offset]) +
               this._u8(bytes[offset + 1]) * 256 +
               this._u8(bytes[offset + 2]) * 65536 +
               this._u8(bytes[offset + 3]) * 16777216;
    },

    '_setU8': function(bytes, offset, value) {
        var v = value & 0xFF;
        bytes[offset] = v > 127 ? v - 256 : v;
    },

    '_patchInt32LE': function(bytes, offset, value) {
        this._setU8(bytes, offset, value & 0xFF);
        this._setU8(bytes, offset + 1, (value >> 8) & 0xFF);
        this._setU8(bytes, offset + 2, (value >> 16) & 0xFF);
        this._setU8(bytes, offset + 3, (value >> 24) & 0xFF);
    },

    '_extractError': function(body) {
        try {
            var j = JSON.parse(body);
            if (j && j.error && j.error.message) return String(j.error.message);
            if (j && j.message) return String(j.message);
        } catch (e) {}
        return String(body || '').substring(0, 500);
    },

    '_cleanText': function(s) {
        return String(s || '').replace(/\r/g, '\n').replace(/\n{3,}/g, '\n\n').trim();
    },

    '_trim': function(v) {
        if (v === null || typeof v === 'undefined') return '';
        return String(v).replace(/^\s+|\s+$/g, '');
    },

    '_u8': function(b) {
        return b < 0 ? b + 256 : b;
    },

    '_hasMagic': function(bytes, a, b, c, d) {
        if (!bytes || bytes.length < 4) return false;
        return this._u8(bytes[0]) === a && this._u8(bytes[1]) === b && this._u8(bytes[2]) === c && this._u8(bytes[3]) === d;
    },

    '_ensurePlayableAudio': function(bytes) {
        if (!bytes || bytes.length < 4) return bytes;
        if (this._hasMagic(bytes, 0x52, 0x49, 0x46, 0x46)) return bytes; // RIFF/WAV
        if (this._hasMagic(bytes, 0x49, 0x44, 0x33, 0x00)) return bytes; // ID3, rare 4th check will fail for many; handled below
        if (this._u8(bytes[0]) === 0x49 && this._u8(bytes[1]) === 0x44 && this._u8(bytes[2]) === 0x33) return bytes; // MP3 ID3
        if (this._u8(bytes[0]) === 0xFF && (this._u8(bytes[1]) & 0xE0) === 0xE0) return bytes; // MP3 frame
        if (this._hasMagic(bytes, 0x4F, 0x67, 0x67, 0x53)) return bytes; // OggS
        if (this._hasMagic(bytes, 0x66, 0x4C, 0x61, 0x43)) return bytes; // fLaC
        return this._pcmToWav(bytes, 24000);
    },

    '_pcmToWav': function(pcmData, sampleRate) {
        var dataSize = pcmData.length;
        var baos = new java.io.ByteArrayOutputStream();
        baos.write(0x52); baos.write(0x49); baos.write(0x46); baos.write(0x46);
        this._writeInt32(baos, 36 + dataSize);
        baos.write(0x57); baos.write(0x41); baos.write(0x56); baos.write(0x45);
        baos.write(0x66); baos.write(0x6D); baos.write(0x74); baos.write(0x20);
        this._writeInt32(baos, 16);
        this._writeInt16(baos, 1);
        this._writeInt16(baos, 1);
        this._writeInt32(baos, sampleRate);
        this._writeInt32(baos, sampleRate * 2);
        this._writeInt16(baos, 2);
        this._writeInt16(baos, 16);
        baos.write(0x64); baos.write(0x61); baos.write(0x74); baos.write(0x61);
        this._writeInt32(baos, dataSize);
        baos.write(pcmData, 0, dataSize);
        var wav = baos.toByteArray();
        baos.close();
        return wav;
    },

    '_writeInt32': function(baos, value) {
        baos.write(value & 0xFF);
        baos.write((value >> 8) & 0xFF);
        baos.write((value >> 16) & 0xFF);
        baos.write((value >> 24) & 0xFF);
    },

    '_writeInt16': function(baos, value) {
        baos.write(value & 0xFF);
        baos.write((value >> 8) & 0xFF);
    }
};

var EditorJS = {
    'getAudioSampleRate': function(locale, voice) { return 24000; },
    'getLocales': function() { return ['zh', 'en']; },
    'getVoices': function(locale) {
        return PluginJS.VOICES;
    },
    'onLoadData': function() {
        var data = ttsrv.tts.data;
        data['audio_format'] = 'wav';
        if (typeof data['style_tag'] === 'undefined') data['style_tag'] = '';
        if (typeof data['user_prompt'] === 'undefined') data['user_prompt'] = '';
        if (typeof data['voice_design_prompt'] === 'undefined') data['voice_design_prompt'] = '';
        if (typeof data['tail_silence_ms'] === 'undefined') data['tail_silence_ms'] = '900';
        if (typeof data['end_guard_text'] === 'undefined') data['end_guard_text'] = '……';
    },
    'onLoadUI': function(ctx, linearLayout) {
        this.onLoadData();

        function addLabel(t) {
            var tv = new android.widget.TextView(ctx);
            tv.setText(t);
            tv.setTextColor(android.graphics.Color.parseColor('#009688'));
            tv.setPadding(0, 26, 0, 8);
            linearLayout.addView(tv);
        }
        function addHelp(t) {
            var tv = new android.widget.TextView(ctx);
            tv.setText(t);
            tv.setTextSize(12);
            tv.setTextColor(android.graphics.Color.parseColor('#757575'));
            tv.setPadding(0, 0, 0, 10);
            linearLayout.addView(tv);
        }
        function bindText(view, key) {
            view.addTextChangedListener(new android.text.TextWatcher({
                afterTextChanged: function(s) { ttsrv.tts.data[key] = s.toString(); },
                beforeTextChanged: function(){},
                onTextChanged: function(){}
            }));
        }

        addLabel('插件类型');
        addHelp('这是普通+VoiceDesign轻量插件，不包含 VoiceClone/URL 下载；获取声音列表会更快。API Key 在插件列表右侧三个点 → 设置变量 中填写。');

        addLabel('响度');
        addHelp('v25 已内置响度归一化(自动把音频峰值拉满)，默认音量即饱满。若仍想更大，在插件列表音频参数里把「音量」调大即可(软限幅防破音)；请在插件列表勾选「插件已处理音量」以获得最稳的控制效果。');

        addLabel('自然语言风格提示（普通音色可选）');
        var etUser = new android.widget.EditText(ctx);
        etUser.setHint('例：用轻快上扬的语调朗读，语速稍快。');
        etUser.setText(String(ttsrv.tts.data['user_prompt'] || ''));
        etUser.setSingleLine(false);
        linearLayout.addView(etUser);
        bindText(etUser, 'user_prompt');

        addLabel('风格标签（普通音色可选）');
        var etTag = new android.widget.EditText(ctx);
        etTag.setHint('例：开心 东北话 唱歌');
        etTag.setText(String(ttsrv.tts.data['style_tag'] || ''));
        linearLayout.addView(etTag);
        bindText(etTag, 'style_tag');

        var btnTags = new android.widget.Button(ctx);
        btnTags.setText('插入快捷标签');
        linearLayout.addView(btnTags);
        btnTags.setOnClickListener(new android.view.View.OnClickListener({
            onClick: function(v) {
                var builder = new android.app.AlertDialog.Builder(ctx);
                builder.setTitle('选择标签');
                builder.setItems(PluginJS.STYLE_TAGS, new android.content.DialogInterface.OnClickListener({
                    onClick: function(dialog, which) {
                        var current = etTag.getText().toString();
                        if (current.length > 0) current += ' ';
                        current += PluginJS.STYLE_TAGS[which];
                        etTag.setText(current);
                        ttsrv.tts.data['style_tag'] = current;
                    }
                }));
                builder.show();
            }
        }));

        addLabel('VoiceDesign 自定义音色描述');
        addHelp('当声音选择 VoiceDesign｜自定义描述 时，优先使用这里的文字。选择其他 VoiceDesign 预设时，也会优先使用这里的文字。');
        var etDesign = new android.widget.EditText(ctx);
        etDesign.setHint('例：一位语速舒缓、声音醇厚的深夜电台男主播。');
        etDesign.setText(String(ttsrv.tts.data['voice_design_prompt'] || ''));
        etDesign.setSingleLine(false);
        linearLayout.addView(etDesign);
        bindText(etDesign, 'voice_design_prompt');

        addLabel('尾端防吞字');
        addHelp('默认在生成文本末尾加短停顿，并给 WAV 音频尾部补 900ms 静音，缓解播放器或接口把最后几个字截掉。吞尾明显可改为 1200；想关闭文本停顿保护就清空“结尾保护符”。');
        var etTail = new android.widget.EditText(ctx);
        etTail.setHint('900');
        etTail.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etTail.setText(String(ttsrv.tts.data['tail_silence_ms'] || '900'));
        linearLayout.addView(etTail);
        bindText(etTail, 'tail_silence_ms');

        var etGuard = new android.widget.EditText(ctx);
        etGuard.setHint('……');
        etGuard.setText(String(ttsrv.tts.data['end_guard_text'] || '……'));
        linearLayout.addView(etGuard);
        bindText(etGuard, 'end_guard_text');

        addLabel('输出格式');
        addHelp('固定走普通 OpenAI 兼容接口：https://api.xiaomimimo.com/v1/chat/completions。v24+ 固定请求 wav，便于追加尾部静音。');
    }
};
/*
 * v25 新增：_s16 / _applyGain
 *  _applyGain(bytes, volume)
 *    1) 解析 WAV data 块，统计 16bit 峰值；
 *    2) 归一化到约 90% 满量程(留余量防爆音)，解决 MiMo 普通接口母带偏轻、默认音量小的问题；
 *    3) 叠加传入的 volume(滑块值，上限 4)，配合 tanh 软限幅避免硬削波破音；
 *    4) 仅处理 WAV，其余格式原样返回。
 *  调用链：getAudio → _postTTS(requestBody, apiKey, volume) → _applyGain → _appendTailSilence
 */