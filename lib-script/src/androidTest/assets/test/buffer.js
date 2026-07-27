let str = "Hello, World !"
let buf = Buffer.from(str, 'utf-8') // Support type: utf-8, base64, hex, ascii.
let arr = new Uint8Array(buf)
console.log("arr", arr) // {"0":72,"1":101,"2":108,"3":108,"4":111,"5":44,"6":32,"7":87,"8":111,"9":114,"10":108,"11":100,"12":32,"13":33}
console.log('buf', buf) // {"0":72,"1":101,"2":108,"3":108,"4":111,"5":44,"6":32,"7":87,"8":111,"9":114,"10":108,"11":100,"12":32,"13":33}

let bs64 = buf.toString('base64')
console.log("base64", bs64) // SGVsbG8sIFdvcmxkICE=

let origin = Buffer.from(bs64, 'base64').toString('utf-8')
console.log("origin", origin) // Hello, World !

let hex = buf.toString('hex')
console.log("hex", hex) // 48656c6c6f2c20576f726c642021

