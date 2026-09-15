package com.crm.backend.attachment;

public class AttachmentUploadsDisabledException extends RuntimeException {

    public AttachmentUploadsDisabledException() {
        super("File uploads are disabled in this environment");
    }
}
