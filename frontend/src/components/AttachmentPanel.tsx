import { useCallback, useEffect, useRef, useState, type ChangeEvent } from 'react'
import toast from 'react-hot-toast'
import { Download, FileText, Paperclip, Trash2, UploadCloud } from 'lucide-react'
import { EmptyState, LoadingState } from './ui'
import {
  deleteAttachment,
  downloadAttachment,
  getCustomerAttachments,
  getLeadAttachments,
  uploadCustomerAttachment,
  uploadLeadAttachment,
  type AttachmentResponse,
} from '../services/attachmentService'
import { formatDateTime } from '../lib/formatters'

type AttachmentPanelProps = {
  ownerType: 'customer' | 'lead'
  ownerId: number
}

function formatFileSize(sizeBytes: number) {
  if (sizeBytes < 1024) {
    return `${sizeBytes} B`
  }

  if (sizeBytes < 1024 * 1024) {
    return `${(sizeBytes / 1024).toFixed(1)} KB`
  }

  return `${(sizeBytes / 1024 / 1024).toFixed(1)} MB`
}

export function AttachmentPanel({ ownerType, ownerId }: AttachmentPanelProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const [attachments, setAttachments] = useState<AttachmentResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)

  const loadAttachments = useCallback(async () => {
    setLoading(true)

    try {
      const response =
        ownerType === 'customer'
          ? await getCustomerAttachments(ownerId, 0, 10)
          : await getLeadAttachments(ownerId, 0, 10)

      setAttachments(response.content)
    } catch {
      toast.error('Could not load attachments')
      setAttachments([])
    } finally {
      setLoading(false)
    }
  }, [ownerId, ownerType])

  useEffect(() => {
    loadAttachments()
  }, [loadAttachments])

  async function handleFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]

    if (!file) {
      return
    }

    setUploading(true)

    try {
      const uploaded =
        ownerType === 'customer'
          ? await uploadCustomerAttachment(ownerId, file)
          : await uploadLeadAttachment(ownerId, file)

      setAttachments((current) => [uploaded, ...current])
      toast.success('Attachment uploaded')
    } catch {
      toast.error('Could not upload attachment')
    } finally {
      setUploading(false)
      event.target.value = ''
    }
  }

  async function handleDelete(attachment: AttachmentResponse) {
    const confirmed = window.confirm(`Delete ${attachment.originalFileName}?`)

    if (!confirmed) {
      return
    }

    setDeletingId(attachment.id)

    try {
      await deleteAttachment(attachment.id)
      setAttachments((current) => current.filter((item) => item.id !== attachment.id))
      toast.success('Attachment deleted')
    } catch {
      toast.error('Could not delete attachment')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <section className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="flex items-center gap-2 font-semibold text-[var(--crm-text)]">
            <Paperclip size={17} />
            Files
          </h3>
          <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
            Store documents, images, and useful account files.
          </p>
        </div>

        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
          className="inline-flex h-9 items-center justify-center gap-2 rounded-xl bg-[var(--crm-primary)] px-3 text-xs font-semibold text-white shadow-lg shadow-violet-500/20 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
        >
          <UploadCloud size={15} />
          {uploading ? 'Uploading...' : 'Upload'}
        </button>

        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          accept=".pdf,.png,.jpg,.jpeg,.txt,.csv,.docx,.xlsx"
          onChange={handleFileSelected}
        />
      </div>

      <div className="mt-4">
        {loading ? (
          <LoadingState message="Loading files..." />
        ) : attachments.length === 0 ? (
          <EmptyState
            icon={FileText}
            title="No files yet"
            message="Upload contracts, proposals, or supporting documents here."
          />
        ) : (
          <div className="space-y-2">
            {attachments.map((attachment) => (
              <div
                key={attachment.id}
                className="flex items-center justify-between gap-3 rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 py-3"
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-[var(--crm-text)]">
                    {attachment.originalFileName}
                  </p>
                  <p className="mt-1 text-xs text-[var(--crm-text-muted)]">
                    {formatFileSize(attachment.sizeBytes)} - Uploaded by {attachment.uploadedByName} -{' '}
                    {formatDateTime(attachment.createdAt)}
                  </p>
                </div>

                <div className="flex shrink-0 items-center gap-2">
                  <button
                    type="button"
                    onClick={() => downloadAttachment(attachment)}
                    className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-[var(--crm-border)] text-[var(--crm-text-muted)] transition hover:border-violet-300 hover:bg-violet-500/10 hover:text-[var(--crm-primary)]"
                    aria-label={`Download ${attachment.originalFileName}`}
                  >
                    <Download size={15} />
                  </button>

                  <button
                    type="button"
                    onClick={() => handleDelete(attachment)}
                    disabled={deletingId === attachment.id}
                    className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-[var(--crm-border)] text-[var(--crm-text-muted)] transition hover:border-rose-300 hover:bg-rose-500/10 hover:text-rose-500 disabled:cursor-not-allowed disabled:opacity-50"
                    aria-label={`Delete ${attachment.originalFileName}`}
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}
