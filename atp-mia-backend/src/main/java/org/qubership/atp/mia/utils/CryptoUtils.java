/*
 *  Copyright 2024-2025 NetCracker Technology Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.qubership.atp.mia.utils;

import javax.annotation.PostConstruct;

import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.api.Encryptor;
import org.qubership.atp.mia.exceptions.authorization.DecryptEncriptedDataException;
import org.qubership.atp.mia.exceptions.authorization.EncryptDataException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class CryptoUtils {

    private static Decryptor decryptorStatic;
    private static Encryptor encryptorStatic;

    private final Decryptor decryptor;
    private final Encryptor encryptor;

    /**
     * Decrypt text if encrypted.
     *
     * @param textToDecrypt text to decrypt
     * @return decrypted text
     */
    public static String decryptValue(String textToDecrypt) {
        try {
            return decryptorStatic.decryptIfEncrypted(textToDecrypt);
        } catch (Exception e) {
            throw new DecryptEncriptedDataException(e);
        }
    }

    /**
     * Encrypt text if not encrypted.
     *
     * @param textToEncrypt text to encrypt
     * @return encrypted text
     */
    public static String encryptValue(String textToEncrypt) {
        try {
            return encryptorStatic.encrypt(textToEncrypt);
        } catch (Exception e) {
            throw new EncryptDataException(e);
        }
    }

    @PostConstruct
    public void init() {
        CryptoUtils.decryptorStatic = decryptor;
        CryptoUtils.encryptorStatic = encryptor;
    }

    /**
     * For test usage.
     *
     * @param decryptor decryptor
     * @param encryptor encryptor
     */
    public void initForTest(Decryptor decryptor, Encryptor encryptor) {
        CryptoUtils.decryptorStatic = decryptor;
        CryptoUtils.encryptorStatic = encryptor;
    }
}
