# Http

## GET
``` javascript
let resp = http.get("https://reqres.in/api/users")
console.log("url", resp.url)
console.log("redirected", resp.redirected)
console.log("isOK", resp.ok)
console.log("status", resp.status)
console.log("statusText", resp.statusText)
console.log("headers", resp.headers)

// 三选一 且不可重复调用
console.log("json", resp.json(true/*不检查状态码*/).data[0].avatar) // https://reqres.in/img/faces/1-image.jpg
// console.log("bytes", resp.bytes())
// console.log("text", resp.text())
```
## POST

```javascript
let body = {
   "email": "eve.holt@reqres.in",
   "password": "cityslicka"
}
let headers = {
   'Content-Type': 'application/json'
}
let resp = http.post('https://reqres.in/api/login', JSON.stringify(body), headers)
let ret = resp.json(true/*不检查状态码*/)

console.log(ret['token']) // QpwL5tke4Pnpja7X4
```

## POST multipart
```javascript
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
```