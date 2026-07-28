let txt = `Hello World! 这是一个文件名为hello.txt的内容`

let partBody = {
   'fileToUpload': { // 文件类型的内容
       'body': txt,
       'fileName': "hello.txt",
       'contentType': "text/plain"
   },
   'time': '1h',
   'reqtype': 'fileupload',
}
let headers = {
   'Content-Type': 'multipart/form-data',
   'User-Agent': 'PostmanRuntime-ApipostRuntime/1.1.0'
}
let resp = http.post('https://catbox.moe/user/api.php', partBody)
let ret = resp.text()


let url = 'https://sy.mgz6.com/shuyuan'
resp = upload(url, "config")
console.log(resp.text())

function upload(url, config, extra) {
    let form = {
        "file":{
            'body': config,
            'fileName': 'config.json',
            'contentType': 'application/json',
          }
    }

    return http.post(url, form)
}