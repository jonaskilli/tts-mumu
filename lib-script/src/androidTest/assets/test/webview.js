let headers = {'User-Agent': 'TTS Server'}
let html = webview.loadUrl('https://cn.bing.com', headers)
console.log('html', html)

let resp = http.get('https://cn.bing.com')
let html2 = resp.text()
console.log('get html', html2)

let js = `document.documentElement.outerHTML` // default js code
html = webview.loadHtml(html2, headers, js)
console.log("loadHtml", html)