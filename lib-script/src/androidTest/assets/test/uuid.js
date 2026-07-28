const numbers = [
  0x6e,
  0xc0,
  0xbd,
  0x7f,
  0x11,
  0xc0,
  0x43,
  0xda,
  0x97,
  0x5e,
  0x2a,
  0x8a,
  0xd9,
  0xeb,
  0xae,
  0x0b
];
const uuidBytes = new Uint8Array(numbers);

// from UInt8Array
let ret = UUID.stringify(uuidBytes); // ⇨ '6ec0bd7f-11c0-43da-975e-2a8ad9ebae0b'
println("stringify: " + ret)
println("validate: " + UUID.validate(ret)) // true
println("validate: ", UUID.validate(ret.replaceAll("-"))) // false

// to UInt8Array
ret = UUID.parse("6ec0bd7f-11c0-43da-975e-2a8ad9ebae0b")
println("bytes: ", ret)

println("v4: " + UUID.v4())

