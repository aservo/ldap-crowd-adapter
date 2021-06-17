package test.api;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;


public class CertificateGenerator {

    public static KeyPair generateKeyPair() {

        try {

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");

            keyPairGenerator.initialize(1024, new SecureRandom());

            return keyPairGenerator.generateKeyPair();

        } catch (NoSuchAlgorithmException |
                NoSuchProviderException e) {

            throw new RuntimeException(e);
        }

    }

    public static X509Certificate generateX509Certificate(KeyPair keyPair, String dnName, long validityDays) {

        try {

            Date validityBeginDate = new Date(System.currentTimeMillis() - 60 * 1000);
            Date validityEndDate = new Date(System.currentTimeMillis() + validityDays * 24 * 60 * 60 * 1000);

            X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
            X500Principal dnNameObject = new X500Principal(dnName);

            generator.setSerialNumber(BigInteger.valueOf(new Random().nextInt() & 0x7fffffff));
            generator.setSubjectDN(dnNameObject);
            generator.setIssuerDN(dnNameObject);
            generator.setNotBefore(validityBeginDate);
            generator.setNotAfter(validityEndDate);
            generator.setPublicKey(keyPair.getPublic());
            generator.setSignatureAlgorithm("SHA256WithRSAEncryption");

            return generator.generate(keyPair.getPrivate(), "BC");

        } catch (CertificateException |
                NoSuchAlgorithmException |
                NoSuchProviderException |
                SignatureException |
                InvalidKeyException e) {

            throw new RuntimeException(e);
        }
    }

    public static void updateKeyStore(KeyPair keyPair,
                                      X509Certificate cert,
                                      String alias,
                                      File keyStoreFile,
                                      String keyStorePassword)
            throws IOException {

        try {

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            X509Certificate[] chain = {cert};
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {

                inputStream = new FileInputStream(keyStoreFile);
                keyStore.load(inputStream, keyStorePassword.toCharArray());

            } catch (FileNotFoundException e) {

                keyStore.load(null, null);

            } finally {

                if (inputStream != null)
                    inputStream.close();
            }

            keyStore.setKeyEntry(alias, keyPair.getPrivate(), keyStorePassword.toCharArray(), chain);

            try {

                outputStream = new FileOutputStream(keyStoreFile);
                keyStore.store(outputStream, keyStorePassword.toCharArray());

            } finally {

                if (outputStream != null)
                    outputStream.close();
            }

        } catch (CertificateException |
                NoSuchAlgorithmException |
                KeyStoreException e) {
        }
    }
}
