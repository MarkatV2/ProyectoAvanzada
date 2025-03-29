package co.edu.uniquindio.proyecto.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.Base64;

/**
 * Utilidad para la carga y gestión de claves RSA públicas y privadas.
 * <p>
 * Esta clase se encarga de inicializar y proveer las claves necesarias para la generación y verificación de JWT.
 * Las claves se cargan a partir de archivos PEM ubicados en el classpath.
 * </p>
 */
@Component
@Slf4j
public class KeyUtils {

    /**
     * -- GETTER --
     *  Retorna la clave pública RSA.
     *
     * @return Clave pública RSA.
     */
    @Getter
    private static RSAPublicKey publicKey;
    /**
     * -- GETTER --
     *  Retorna la clave privada RSA.
     *
     * @return Clave privada RSA.
     */
    @Getter
    private static RSAPrivateKey privateKey;
    private final ResourceLoader resourceLoader;

    /**
     * Constructor que inyecta el ResourceLoader para acceder a los archivos de claves.
     *
     * @param resourceLoader Inyección del cargador de recursos, cualificado con el contexto web.
     */
    public KeyUtils(@Qualifier("webApplicationContext") ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        initializeKeys();
    }

    /**
     * Inicializa las claves públicas y privadas a partir de los archivos PEM.
     * <p>
     * Si ocurre algún error durante la carga, se lanza una RuntimeException.
     * </p>
     */
    private void initializeKeys() {
        try {
            Resource publicKeyResource = resourceLoader.getResource("classpath:keys/public-key.pem");
            Resource privateKeyResource = resourceLoader.getResource("classpath:keys/private-key.pem");

            byte[] publicKeyBytes = publicKeyResource.getInputStream().readAllBytes();
            byte[] privateKeyBytes = privateKeyResource.getInputStream().readAllBytes();

            publicKey = readPublicKey(publicKeyBytes);
            privateKey = readPrivateKey(privateKeyBytes);
            log.info("Claves RSA cargadas exitosamente.");
        } catch (Exception e) {
            log.error("Error cargando las claves RSA", e);
            throw new RuntimeException("Error cargando las keys", e);
        }
    }

    /**
     * Lee y convierte el contenido de un archivo PEM en una clave pública RSA.
     *
     * @param keyBytes Arreglo de bytes con el contenido del archivo PEM.
     * @return Clave pública RSA.
     * @throws Exception Si ocurre algún error durante la conversión.
     */
    private RSAPublicKey readPublicKey(byte[] keyBytes) throws Exception {
        String publicKeyPEM = new String(keyBytes)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    /**
     * Lee y convierte el contenido de un archivo PEM en una clave privada RSA.
     *
     * @param keyBytes Arreglo de bytes con el contenido del archivo PEM.
     * @return Clave privada RSA.
     * @throws Exception Si ocurre algún error durante la conversión.
     */
    private RSAPrivateKey readPrivateKey(byte[] keyBytes) throws Exception {
        String privateKeyPEM = new String(keyBytes)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }


}
