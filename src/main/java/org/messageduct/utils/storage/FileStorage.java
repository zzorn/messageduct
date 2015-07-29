package org.messageduct.utils.storage;

import org.flowutils.serializer.KryoSerializer;
import org.flowutils.serializer.Serializer;
import org.messageduct.utils.FileUtils;
import org.messageduct.utils.encryption.AesEncryption;
import org.messageduct.utils.encryption.SymmetricEncryption;
import org.messageduct.utils.encryption.WrongPasswordException;

import java.io.*;

import static org.flowutils.Check.notNull;

/**
 * Utility that serializes data to and from a file.
 * Uses a temporary file to avoid corruption if the program is aborted in the middle of writing.
 * Can optionally use encryption.
 *
 * Synchronizes save and load access, so can be used from multiple threads.
 */
public final class FileStorage extends SynchronizedStorage {

    private final File file;
    private final File tempFile;

    private final Serializer serializer;
    private final SymmetricEncryption symmetricEncryption;
    private final char[] password;


    /**
     * Creates a new file based Storage with a default serializer and no encryption.
     *
     * @param file file to save the data to.
     */
    public FileStorage(File file) {
        this(file, null, new KryoSerializer(false), null);
    }

    /**
     * Creates a new file based Storage with a default encryption provider and serializer.
     *
     * @param file file to save the data to.
     * @param password password to use for encrypting the file.  If null, no encryption is done.
     */
    public FileStorage(File file, char[] password) {
        this(file, password, new KryoSerializer(false));
    }

    /**
     * Creates a new file based Storage with a default encryption provider.
     *
     * @param file file to save the data to.
     * @param password password to use for encrypting the file.  If null, no encryption is done.
     * @param serializer serializer used to serialize the object saved.
     */
    public FileStorage(File file, char[] password, Serializer serializer) {
        this(file, password, serializer, new AesEncryption(), FileUtils.createTempFileName(file));
    }

    /**
     * Creates a new file based Storage with a default encryption provider.
     *
     * @param file file to save the data to.
     * @param password password to use for encrypting the file.  If null, no encryption is done.
     * @param serializer serializer used to serialize the object saved.
     * @param symmetricEncryption encryption provider to use for encrypting the file, or null if no encryption should be done.
     *                           If a non-null password is provided, an encryptionProvider has to be provided.
     */
    public FileStorage(File file, char[] password, Serializer serializer, SymmetricEncryption symmetricEncryption) {
        this(file, password, serializer, symmetricEncryption, FileUtils.createTempFileName(file));
    }

    /**
     * Creates a new file based Storage.
     *
     * @param file file to save the data to.
     * @param password password to use for encrypting the file.  If null, no encryption is done.
     * @param serializer serializer used to serialize the object saved.
     * @param symmetricEncryption encryption provider to use for encrypting the file, or null if no encryption should be done.
     *                           If a non-null password is provided, an encryptionProvider has to be provided.
     * @param tempFile temporary file to save to first, to avoid corruption if there is a problem in the middle of saving.
     *                 By default the same name as the datafile, except with a ".temp" appended.
     */
    public FileStorage(File file, char[] password, Serializer serializer, SymmetricEncryption symmetricEncryption, File tempFile) {
        notNull(file, "file");
        notNull(tempFile, "tempFile");
        notNull(serializer, "serializer");
        if (password != null && symmetricEncryption == null) throw new IllegalArgumentException("If a password is provided, an encryptionProvider has to be given as well, but encryptionProvider was null");

        this.file = file;
        this.tempFile = tempFile;
        this.serializer = serializer;
        this.symmetricEncryption = symmetricEncryption;
        this.password = password;
    }

    @Override protected void doSave(Object object) throws IOException {
        // Serialize the object
        byte[] data = serializer.serialize(object);

        // Encrypt if we have a password
        if (symmetricEncryption != null && password != null) {
            data = symmetricEncryption.encrypt(data, password);
        }

        // Save the data, making sure it actually got saved
        FileUtils.saveAndCheck(data, file, tempFile);
    }


    @Override protected <T> T doLoad() throws IOException {
        // If there is no file, the storage has not yet been used
        if (!file.exists()) return null;

        // Load file contents
        byte[] data = FileUtils.loadData(file);

        // Decrypt if we have password specified
        if (symmetricEncryption != null && password != null) {
            try {
                data = symmetricEncryption.decrypt(data, password);
            } catch (WrongPasswordException e) {
                throw new IOException("Wrong password used for attempting to decrypt the storage: " + e.getMessage(), e);
            }
        }

        // Deserialize
        return serializer.deserialize(data);
    }

}
