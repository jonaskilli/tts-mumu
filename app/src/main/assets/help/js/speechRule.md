# 朗读规则
> 朗读规则用于将原始文本分段并标记tag，然后由TTS Server根据tag匹配配置进行语音合成。

## 软件内置的规则示例：

```javascript 
let SpeechRuleJS = {
    name: "旁白/对话",
    id: "ttsrv.multi_voice",
    author: "TTS Server",
    version: 4,
    
    // Map类型，key为标签id，value为标签下拉框中的显示名称
    tags: {narration: "旁白", dialogue: "对话"},

    // text 为原始的未经处理的文本
    // 返回一个List：
    // [
    //     {text: "这是一段旁白文本", tag: "narration"}, 
    //     {text: "这是一段对话文本", tag: "dialogue"}
    // ]
    handleText(text) {
        const list = [];
        let tmpStr = "";
        let endTag = "narration";

        text.split("").forEach((char, index) => {
            tmpStr += char;

            if (char === '“') {
                endTag = "dialogue";
                list.push({text: tmpStr, tag: "narration"});
                tmpStr = "";
            } else if (char === '”') {
                endTag = "narration";
                tmpStr = tmpStr.slice(0, -1)
                list.push({text: tmpStr, tag: "dialogue"});
                tmpStr = "";
            } else if (index === text.length - 1) {
                list.push({text: tmpStr, tag: endTag});
            }
        });

        return list;
    },

    // 调用完handleText后对其返回的list中每个元素text进行分句，
    // 返回一个字符串List
    splitText(text) {
        let separatorStr = "。？?！!;；"

        let list = []
        let tmpStr = ""
        text.split("").forEach((char, index) => {
            tmpStr += char

            if (separatorStr.includes(char)) {
                list.push(tmpStr)
                tmpStr = ""
            } else if (index === text.length - 1) {
                list.push(tmpStr);
            }
        })

        return list.filter(item =>  item.replace(/[“”]/g, '').trim().length > 0);
    }

};


```

