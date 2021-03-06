/*
 * JPass
 *
 * Copyright (c) 2009-2017 Gabor Bata
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpass.ui.helper;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import org.apache.commons.lang.StringUtils;
import jpass.crypt.RsaOaep;
import jpass.ui.EntryDialog;
import jpass.ui.JPassFrame;
import jpass.ui.MessageDialog;
import jpass.ui.TextMessageEncryptionDialog;
import jpass.util.ClipboardUtils;
import jpass.xml.bind.Entry;

/**
 * Helper class for entry operations.
 *
 * @author Gabor_Bata
 *
 */
public final class EntryHelper {

    private EntryHelper() {
        // not intended to be instantiated
    }

    /**
     * Deletes an entry.
     *
     * @param parent parent component
     */
    public static void deleteEntry(JPassFrame parent) {
        if (parent.getEntryTitleList().getSelectedIndex() == -1) {
            MessageDialog.showWarningMessage(parent, "Please select an entry.");
            return;
        }
        int option = MessageDialog.showQuestionMessage(parent, "Do you really want to delete this entry?",
                MessageDialog.YES_NO_OPTION);
        if (option == MessageDialog.YES_OPTION) {
            String title = (String) parent.getEntryTitleList().getSelectedValue();
            parent.getModel().getEntries().getEntry().remove(parent.getModel().getEntryByTitle(title));
            parent.getModel().setModified(true);
            parent.refreshFrameTitle();
            parent.refreshEntryTitleList(null);
        }
    }

    /**
     * Duplicates an entry.
     *
     * @param parent parent component
     */
    public static void duplicateEntry(JPassFrame parent) {
        if (parent.getEntryTitleList().getSelectedIndex() == -1) {
            MessageDialog.showWarningMessage(parent, "Please select an entry.");
            return;
        }
        String title = (String) parent.getEntryTitleList().getSelectedValue();
        Entry oldEntry = parent.getModel().getEntryByTitle(title);
        EntryDialog ed = new EntryDialog(parent, "Duplicate Entry", oldEntry, true);
        if (ed.getFormData() != null) {
            parent.getModel().getEntries().getEntry().add(ed.getFormData());
            parent.getModel().setModified(true);
            parent.refreshFrameTitle();
            parent.refreshEntryTitleList(ed.getFormData().getTitle());
        }
    }

    /**
     * Edits the entry.
     *
     * @param parent parent component
     */
    public static void editEntry(JPassFrame parent) {
        if (parent.getEntryTitleList().getSelectedIndex() == -1) {
            MessageDialog.showWarningMessage(parent, "Please select an entry.");
            return;
        }
        String title = (String) parent.getEntryTitleList().getSelectedValue();
        Entry oldEntry = parent.getModel().getEntryByTitle(title);
        EntryDialog ed = new EntryDialog(parent, "Edit Entry", oldEntry, false);
        if (ed.getFormData() != null) {
            parent.getModel().getEntries().getEntry().remove(oldEntry);
            parent.getModel().getEntries().getEntry().add(ed.getFormData());
            parent.getModel().setModified(true);
            parent.refreshFrameTitle();
            parent.refreshEntryTitleList(ed.getFormData().getTitle());
        }
    }
    
    public static void encryptFileWithEntry(JPassFrame parent) {
        if (parent.getEntryTitleList().getSelectedIndex() == -1) {
            MessageDialog.showWarningMessage(parent, "Please select an entry.");
            return;
        }
        String title = (String) parent.getEntryTitleList().getSelectedValue();
        Entry oldEntry = parent.getModel().getEntryByTitle(title);
        if (StringUtils.isEmpty(oldEntry.getPassword())) {
            MessageDialog.showWarningMessage(parent, "The password field of this entry is empty.");
            return;
        }
        FileHelper.encryptFile(parent, oldEntry.getPassword());
    }
    
    public static void decryptFileWithEntry(JPassFrame parent) {
        if (parent.getEntryTitleList().getSelectedIndex() == -1) {
            MessageDialog.showWarningMessage(parent, "Please select an entry.");
            return;
        }
        String title = (String) parent.getEntryTitleList().getSelectedValue();
        Entry oldEntry = parent.getModel().getEntryByTitle(title);
        if (StringUtils.isEmpty(oldEntry.getPassword())) {
            MessageDialog.showWarningMessage(parent, "The password field of this entry is empty.");
            return;
        }
        FileHelper.decryptFile(parent, oldEntry.getPassword());
    }
    
    public static void encryptTextMessage(JPassFrame parent) {
        if (parent.getEntryTitleList().getSelectedIndex() == -1) {
            MessageDialog.showWarningMessage(parent, "Please select an entry.");
            return;
        }
        String title = (String) parent.getEntryTitleList().getSelectedValue();
        Entry oldEntry = parent.getModel().getEntryByTitle(title);
        if (!StringUtils.startsWith(oldEntry.getNotes(), "-----BEGIN RSA PUBLIC KEY-----")) {
            MessageDialog.showWarningMessage(parent, "The entry is not a public key");
            return;
        }
        try {
            RsaOaep rsaOaep = new RsaOaep();
            final String base64 = Arrays.stream(StringUtils.split(oldEntry.getNotes(), '\n'))
                                        .map(l -> l.replaceAll("\\s+","")).filter(l -> !StringUtils.contains(l, "-"))
                                        .collect(Collectors.joining());
            PublicKey publicKey = rsaOaep.deserializePublicKey(base64);
            TextMessageEncryptionDialog textMessageEncryptionDialog = new TextMessageEncryptionDialog(parent, "Text Message Encryption");
            final String textMessage = textMessageEncryptionDialog.getText().orElse(null);
            if (StringUtils.isEmpty(textMessage)) return;
            byte[] randomKey = new byte[128];
            new SecureRandom().nextBytes(randomKey);
            final String noncepass    = Base64.getEncoder().encodeToString(randomKey);
            final String encryptedRsa = rsaOaep.encrypt(publicKey, noncepass, new SecureRandom());
            final String encryptedAes = FileHelper.encryptTextMessage(textMessage, noncepass, UUID.randomUUID().toString().getBytes());
            Entry entry = new Entry() {{
               setTitle("RSA+AES TEXT MESSAGE ENCRYPTION: " + new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date()));
               setUrl(encryptedRsa + "." + encryptedAes);
               setNotes("Your text message was encrypted with the public key and was set in the URL field.\n======================================\n" + textMessage);
            }};
            EntryDialog ed = new EntryDialog(parent, "Add New Entry", entry, false);
            if (ed.getFormData() != null) {
                parent.getModel().getEntries().getEntry().add(ed.getFormData());
                parent.getModel().setModified(true);
                parent.refreshFrameTitle();
                parent.refreshEntryTitleList(ed.getFormData().getTitle());
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showWarningMessage(parent, e.toString());
        }
    }
    
    public static void encryptPhraseWithRsa(JPassFrame parent) {
        if (parent.getEntryTitleList().getSelectedIndex() == -1) {
            MessageDialog.showWarningMessage(parent, "Please select an entry.");
            return;
        }
        String title = (String) parent.getEntryTitleList().getSelectedValue();
        Entry oldEntry = parent.getModel().getEntryByTitle(title);
        if (!StringUtils.startsWith(oldEntry.getNotes(), "-----BEGIN RSA PUBLIC KEY-----")) {
            MessageDialog.showWarningMessage(parent, "The entry is not a public key");
            return;
        }
        try {
            RsaOaep rsaOaep = new RsaOaep();
            final String base64 = Arrays.stream(StringUtils.split(oldEntry.getNotes(), '\n'))
                                        .map(l -> l.replaceAll("\\s+","")).filter(l -> !StringUtils.contains(l, "-"))
                                        .collect(Collectors.joining());
            PublicKey publicKey = rsaOaep.deserializePublicKey(base64);
            final String phrase = JOptionPane.showInputDialog(parent, "Input the pass phrase you want to encrypt.");
            if (StringUtils.isEmpty(phrase)) return;
            final String encryptedPhrase = rsaOaep.encrypt(publicKey, phrase, new SecureRandom());
            Entry entry = new Entry() {{
               setTitle("RSA ENCRYPTION: " + new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date()));
               setNotes(encryptedPhrase);
            }};
            EntryDialog ed = new EntryDialog(parent, "Add New Entry", entry, false);
            if (ed.getFormData() != null) {
                parent.getModel().getEntries().getEntry().add(ed.getFormData());
                parent.getModel().setModified(true);
                parent.refreshFrameTitle();
                parent.refreshEntryTitleList(ed.getFormData().getTitle());
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showWarningMessage(parent, e.toString());
        }
    }
    
    public static void decryptTextMessage(JPassFrame parent) {
        if (parent.getEntryTitleList().getSelectedIndex() == -1) {
            MessageDialog.showWarningMessage(parent, "Please select an entry.");
            return;
        }
        String title = (String) parent.getEntryTitleList().getSelectedValue();
        Entry oldEntry = parent.getModel().getEntryByTitle(title);
        if (!StringUtils.startsWith(oldEntry.getNotes(), "-----BEGIN RSA PRIVATE KEY-----")) {
            MessageDialog.showWarningMessage(parent, "The entry is not a private key");
            return;
        }
        try {
            RsaOaep rsaOaep = new RsaOaep();
            final String base64 = Arrays.stream(StringUtils.split(oldEntry.getNotes(), '\n'))
                                        .map(l -> l.replaceAll("\\s+","")).filter(l -> !StringUtils.contains(l, "-"))
                                        .collect(Collectors.joining());
            PrivateKey privateKey = rsaOaep.deserializePrivateKey(base64);
            TextMessageEncryptionDialog textMessageEncryptionDialog = new TextMessageEncryptionDialog(parent, "Text Message Decryption");
            final String encryptedTextMessage = textMessageEncryptionDialog.getText().orElse(null);
            if (StringUtils.isEmpty(encryptedTextMessage)) return;
            final String decryptedPhrase = rsaOaep.decrypt(privateKey, StringUtils.substringBefore(encryptedTextMessage, ".").replaceAll("\\s+",""));
            final String textMessage 
                = FileHelper.decryptTextMessage(StringUtils.substringAfter(encryptedTextMessage, ".").replaceAll("\\s+",""),
                                                decryptedPhrase,
                                                MessageDialog.readSaltFromTextMessage(encryptedTextMessage));
            Entry entry = new Entry() {{
               setTitle("RSA+AES TEXT MESSAGE DECRYPTION: " + new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date()));
               setNotes(textMessage);
            }};
            EntryDialog ed = new EntryDialog(parent, "Add New Entry", entry, false);
            if (ed.getFormData() != null) {
                parent.getModel().getEntries().getEntry().add(ed.getFormData());
                parent.getModel().setModified(true);
                parent.refreshFrameTitle();
                parent.refreshEntryTitleList(ed.getFormData().getTitle());
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showWarningMessage(parent, e.toString());
        }
    }
    
    public static void decryptPhraseWithRsa(JPassFrame parent) {
        if (parent.getEntryTitleList().getSelectedIndex() == -1) {
            MessageDialog.showWarningMessage(parent, "Please select an entry.");
            return;
        }
        String title = (String) parent.getEntryTitleList().getSelectedValue();
        Entry oldEntry = parent.getModel().getEntryByTitle(title);
        if (!StringUtils.startsWith(oldEntry.getNotes(), "-----BEGIN RSA PRIVATE KEY-----")) {
            MessageDialog.showWarningMessage(parent, "The entry is not a private key");
            return;
        }
        try {
            RsaOaep rsaOaep = new RsaOaep();
            final String base64 = Arrays.stream(StringUtils.split(oldEntry.getNotes(), '\n'))
                                        .map(l -> l.replaceAll("\\s+","")).filter(l -> !StringUtils.contains(l, "-"))
                                        .collect(Collectors.joining());
            PrivateKey privateKey = rsaOaep.deserializePrivateKey(base64);
            final String phrase = JOptionPane.showInputDialog(parent, "Input the pass phrase you want to decrypt.");
            if (StringUtils.isEmpty(phrase)) return;
            final String decryptedPhrase = rsaOaep.decrypt(privateKey, phrase.replaceAll("\\s+",""));
            Entry entry = new Entry() {{
               setTitle("RSA DECRYPTION: " + new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date()));
               setPassword(decryptedPhrase);
            }};
            EntryDialog ed = new EntryDialog(parent, "Add New Entry", entry, false);
            if (ed.getFormData() != null) {
                parent.getModel().getEntries().getEntry().add(ed.getFormData());
                parent.getModel().setModified(true);
                parent.refreshFrameTitle();
                parent.refreshEntryTitleList(ed.getFormData().getTitle());
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showWarningMessage(parent, e.toString());
        }
    }
    
    public static void generateRsaKeyPair(JPassFrame parent) {
        String[] keyEntries = new String[]{"rsa.pub", "rsa.pvt"};
        if (parent.getModel().getEntries().getEntry().stream().anyMatch(e -> Arrays.stream(keyEntries).anyMatch(e.getTitle()::equals))) {
            MessageDialog.showWarningMessage(parent, "Key entries already exist.");
            return;
        }
        try {
            RsaOaep rsaOaep = new RsaOaep();
            KeyPair keypair = rsaOaep.createKeyPair(new SecureRandom());
            Entry privateKey = new Entry(){{
                setTitle("rsa.pvt");
                setNotes("-----BEGIN RSA PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(keypair.getPrivate().getEncoded()) + "\n-----END RSA PRIVATE KEY-----");
            }};
            Entry publicKey  = new Entry(){{
                setTitle("rsa.pub");
                setNotes("-----BEGIN RSA PUBLIC KEY-----\n"  + Base64.getEncoder().encodeToString(keypair.getPublic().getEncoded())  + "\n-----END RSA PUBLIC KEY-----");
            }};
            parent.getModel().getEntries().getEntry().add(privateKey);
            parent.getModel().getEntries().getEntry().add(publicKey);
            parent.getModel().setModified(true);
            parent.refreshFrameTitle();
            parent.refreshEntryTitleList("rsa.pub");
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showWarningMessage(parent, e.toString());
        }
    }

    /**
     * Adds an entry.
     *
     * @param parent parent component
     */
    public static void addEntry(JPassFrame parent) {
        EntryDialog ed = new EntryDialog(parent, "Add New Entry", null, true);
        if (ed.getFormData() != null) {
            parent.getModel().getEntries().getEntry().add(ed.getFormData());
            parent.getModel().setModified(true);
            parent.refreshFrameTitle();
            parent.refreshEntryTitleList(ed.getFormData().getTitle());
        }
    }

    /**
     * Gets the selected entry.
     *
     * @param parent the parent frame
     * @return the entry or null
     */
    public static Entry getSelectedEntry(JPassFrame parent) {
        if (parent.getEntryTitleList().getSelectedIndex() == -1) {
            MessageDialog.showWarningMessage(parent, "Please select an entry.");
            return null;
        }
        return parent.getModel().getEntryByTitle((String) parent.getEntryTitleList().getSelectedValue());
    }

    /**
     * Copy entry field value to clipboard.
     *
     * @param parent the parent frame
     * @param content the content to copy
     */
    public static void copyEntryField(JPassFrame parent, String content) {
        try {
            ClipboardUtils.setClipboardContent(content);
        } catch (Exception e) {
            MessageDialog.showErrorMessage(parent, e.getMessage());
        }
    }
}
