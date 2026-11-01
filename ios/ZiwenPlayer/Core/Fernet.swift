import Foundation
import CommonCrypto
import CryptoKit

/// Fernet 解密（AES-128-CBC + HMAC-SHA256），与 Python / Java 端实现互通。
enum Fernet {

    static func decrypt(token: String, key base64Key: String) throws -> String {
        guard let keyData = Data(base64Encoded: base64Key), keyData.count == 32 else {
            throw SiteError.crypto
        }
        guard let tokenData = base64URLDecode(token), tokenData.count >= 57 else {
            throw SiteError.crypto
        }
        guard tokenData[tokenData.startIndex] == 0x80 else {
            throw SiteError.crypto
        }

        let signingKey = keyData.prefix(16)
        let encryptionKey = Data(keyData[16..<32])
        let hmacInput = tokenData.prefix(tokenData.count - 32)
        let signature = Data(tokenData.suffix(32))

        let mac = HMAC<SHA256>.authenticationCode(for: hmacInput, using: SymmetricKey(data: signingKey))
        guard constantTimeEquals(Data(mac), signature) else {
            throw SiteError.crypto
        }

        let ivRange = 9..<25
        let cipherRange = 25..<(tokenData.count - 32)
        let iv = Data(tokenData[ivRange])
        let cipherText = Data(tokenData[cipherRange])

        // Fernet 规范：AES-128-CBC 用的是密钥的后 16 字节（前 16 字节用于 HMAC）
        let plain = try aesCBCDecrypt(cipherText: cipherText, key: encryptionKey, iv: iv)
        guard let text = String(data: plain, encoding: .utf8) else {
            throw SiteError.crypto
        }
        return text
    }

    private static func constantTimeEquals(_ lhs: Data, _ rhs: Data) -> Bool {
        guard lhs.count == rhs.count else { return false }
        var diff: UInt8 = 0
        for index in 0..<lhs.count {
            diff |= lhs[index] ^ rhs[index]
        }
        return diff == 0
    }

    private static func aesCBCDecrypt(cipherText: Data, key: Data, iv: Data) throws -> Data {
        let bufferSize = cipherText.count + kCCBlockSizeAES128
        var buffer = Data(count: bufferSize)
        var moved = 0

        let status = buffer.withUnsafeMutableBytes { bufferBytes -> CCCryptorStatus in
            guard let bufferBase = bufferBytes.bindMemory(to: UInt8.self).baseAddress else {
                return CCCryptorStatus(kCCMemoryFailure)
            }
            return cipherText.withUnsafeBytes { cipherBytes in
                key.withUnsafeBytes { keyBytes in
                    iv.withUnsafeBytes { ivBytes in
                        CCCrypt(
                            CCOperation(kCCDecrypt),
                            CCAlgorithm(kCCAlgorithmAES),
                            CCOptions(kCCOptionPKCS7Padding),
                            keyBytes.baseAddress, key.count,
                            ivBytes.baseAddress,
                            cipherBytes.baseAddress, cipherText.count,
                            bufferBase, bufferSize,
                            &moved
                        )
                    }
                }
            }
        }

        guard status == kCCSuccess else { throw SiteError.crypto }
        return buffer.prefix(moved)
    }

    private static func base64URLDecode(_ value: String) -> Data? {
        var text = value
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        let remainder = text.count % 4
        if remainder > 0 {
            text += String(repeating: "=", count: 4 - remainder)
        }
        return Data(base64Encoded: text, options: .ignoreUnknownCharacters)
    }
}
