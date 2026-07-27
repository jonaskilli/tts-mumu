let bytes = new Uint8Array(2048)
bytes.fill(0)

let name = "fill_1.bin"
let ok = fs.writeFile(name, bytes)

let str = fs.readText(name, 'UTF-8')
console.log(name, str)

name = "hello.txt"
ok = fs.writeFile(name, "hello world!" /*, 'UTF-8'*/ )

str = fs.readText(name)
console.log(name, str)

let newName = name + ".bak"
ok = fs.rename(name, newName)

ok = fs.copy(newName, newName + ".copied", true /* overwrite */)

ok = fs.rm(newName)

ok = fs.mkdir("123")
ok = fs.mkdir("123/456", true /* recursive */)

let exists = fs.exists("123/456")
console.log('exists', exists)

let isFile = fs.isFile("123/456")
console.log('dirs',  !isFile)

isFile = fs.isFile("fill_1.bin")
console.log('file',  isFile)
